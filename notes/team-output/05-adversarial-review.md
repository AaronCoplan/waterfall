# Waterfall — Adversarial Review (Task #5)

Author: skeptic
Date: 2026-05-14
Inputs read in full: `01-codebase-audit.md`, `02-pl-landscape.md`, `03-language-design.md`, `04-strategy.md`, the ANTLR grammar, the symbol table, the relevant `*Data` classes, the JS and C backends, and the examples directory.

This document is the final adversarial review before the quality pass. It evaluates **reasoning and logic** — assumptions, edge cases, internal consistency, and prior-art accuracy — not output polish. Where I found that the team's position holds up, I say so.

---

## Summary table

| Area | FATAL | RISK | MINOR |
|---|---:|---:|---:|
| `readonly` semantics (Section 2 of 03) | 1 | 5 | 3 |
| `readonly` grammar / implementation | 0 | 2 | 1 |
| Other T1 feature design (Sections 1 of 03) | 0 | 3 | 2 |
| Strategy / niche / sequencing (04) | 0 | 4 | 2 |
| Completeness (omitted features) | 0 | 1 | 2 |
| **Total** | **1** | **15** | **10** |

The single FATAL finding is the **lambda-capture-readonly soundness gap (F1)**. Everything else is recoverable.

---

## Findings

### F1 — Lambda-captures-as-readonly is *misframed* and either redundant or hostile.

- **Section reference**: 03 §2a.10 (lines 1240–1278) and Snippet 10 (lines 1786–1798).
- **Severity**: **FATAL** — not because it breaks the language, but because the spec as written is *incoherent with current grammar* and *will surprise every user* the moment the grammar is loosened in P12.
- **What the designer claims**: "all lambda captures are implicitly readonly within the lambda body" (line 1273). The motivation: prevent the "soundness hole" of a lambda defined before the freeze that writes to `x` and is then called after the freeze (lines 1252–1267).
- **The empirical problem (FATAL part)**: in the *current* grammar at `WaterfallParser.g4:139-142`, a lambda body is *exactly one `functionCall`* or empty. I verified this in `LambdaFunctionData.kt:6-18` — the body field is a `FunctionCallData?`. **There is no syntactic way for a lambda body to contain an assignment.** Assignments are statements (`variableAssignment` rule at line 76), and lambda bodies hold expressions, specifically one `functionCall`. So the "soundness hole" the designer is patching *cannot be expressed in v1*. The whole §2a.10 design is solving a problem that the grammar does not allow you to write.
- **The semantic problem (where the trap fires)**: P12 (per strategy 04 §3, line 200) will add "multi-statement lambda bodies." The moment that ships, the implicit-readonly-capture rule becomes load-bearing — and it directly contradicts the rest of §2 of the design. Specifically:
  - §2a.1 (line 1002): "*no write to this name can succeed from this program point forward*"
  - §2a.10: lambda captures are readonly *from the moment of capture*, regardless of whether `readonly x` has been written
  - These are different rules. The first is a *flow-sensitive promotion*. The second is an *implicit, eager freeze on capture*. Calling both "readonly" elides a real semantic distinction.
- **Why it matters**: the user writes
  ```
  int counter = 0
  const inc := () ==> increment(counter)   // wait — this *reads* counter, but suppose grammar later allowed writes
  inc()
  inc()
  // expected: counter == 2
  // verifier: rejects inc()'s body because counter is "readonly" in capture
  ```
  Every JS / Kotlin / Python developer reading the language tutorial in 2028 will write exactly this code, get rejected, and bounce. The lambda-as-counter pattern is *the* canonical use of closures.
- **The deeper logical flaw**: the designer dismisses option 1 ("forbid lambdas that write to captures") as "strict, ugly, but sound" — and then picks option 3 ("treat all captures as readonly") which is *functionally identical to option 1* for any captured variable that is going to be written. The two options differ only in the error message: option 1 says "lambda cannot write to captures"; option 3 says "captured binding is readonly so you can't write to it." Same constraint, worse message.
- **Comparison to peers**:
  - **Rust**: closures default to capturing by reference; `FnMut` is *opt-in* for writes; `move` for ownership transfer. *Three* modes, explicitly chosen.
  - **Kotlin**: lambdas can read *and write* captured vars (they're boxed `Ref<T>` under the hood). Local `val` is unwritable simply because it's a `val`, not because it's captured.
  - **JS / TS / Python**: full read+write on captures, period.
  - **Java**: requires "effectively final" for captures — but that's because Java *erases* the closure and copies the value into the synthetic class. The semantics: read-only-snapshot, not "write rejected." The user can wrap in `AtomicInteger` to mutate.
  - The designer cites Rust's "`Fn` (read-only capture by default)" — this is misleading. Rust's `Fn`/`FnMut`/`FnOnce` is a *trait dispatch* on call signature, not a property of the binding. A closure that writes a capture gets inferred as `FnMut` automatically; the user doesn't write `mut` on the capture site.
- **Suggested fix**: split the rule. Form B is flow-sensitive on the binding. Lambda capture is a *separate* concern: in v1, since multi-statement lambda bodies don't exist, defer this rule entirely — it's vacuous. When P12 lands multi-statement bodies, introduce explicit capture modes (`(... ) ==> { ... }` reads captures, `(... ) ==> mut { ... }` allows writes) or follow Kotlin's "captures are whatever the binding is" model. Either way, do not paper over a future design decision in the v1 spec.

---

### F2 — The "intersection rule" at branch joins is renamed but not *justified* as the right one.

- **Section reference**: 03 §2a.5 (lines 1062–1121).
- **Severity**: RISK.
- **What the designer claims**: "after the if, `x` is readonly only if *both* branches promoted it" (intersection rule, lines 1080–1087). Justified by analogy to Java/C# definite assignment (line 1105).
- **The analogy is wrong**: definite assignment is "the variable is *guaranteed assigned* at the join." That's an *under*-approximation that prevents reads of uninitialized memory. The dual concept here is "the variable is *guaranteed frozen* at the join" — that's also an under-approximation, but the *failure mode* is different.
  - Under-approximating definite-assignment rejects reads of *possibly* uninitialized data. The cost is "compiler is over-strict; can't read this thing."
  - Under-approximating frozen-state rejects… nothing? It just means the variable stays writable. The cost is silent: a write succeeds where the user thought it would be rejected.
- **The failure scenario**:
  ```
  func process(int x) returns int {
      if (x > 0) {
          x = sanitize(x)
          readonly x        // I've sanitized x; lock it in
      } else {
          // x <= 0 fine as-is, no need to freeze
      }
      x = 7                 // accepted — but did the user intend this?
      return useSanitized(x)
  }
  ```
  In Java's definite-assignment terms, this is "x might or might not be sanitized at this point." The intersection rule says "x is mutable" so the `x = 7` line is accepted. But a *defensive* reading is: "the user said `readonly x` in *some* path; respect their intent."
- **Counter to my own argument**: if you adopt the union rule (`x` is readonly if *any* path promoted), you reject `x = 7` even when control never went through the freezing branch. That's worse — false positive.
- **Why the designer's choice is *correct but unjustified***: the right rule is the intersection rule, but the actual justification is not "definite assignment dual" — it's *the principle of least surprise relative to control flow*. The freeze should reach exactly the program points where every preceding path froze. The Java analogy obscures this; pick a cleaner derivation.
- **Suggested fix**: keep intersection but rewrite the justification in §2a.5 to be: "a binding is frozen at a join iff every reachable predecessor froze it. This is structurally the same as definite-assignment analysis (assignedness intersects across joins). The *failure mode* of this rule when wrong is silent: writes succeed where the user expected rejection. The mitigation is good error messages on the rejected case — when the user adds a *second* readonly in the other branch, the verifier should say 'now frozen because all branches promoted'." This makes the rule auditable.

---

### F3 — Branch-join: nested-if cases the designer didn't work through.

- **Section reference**: 03 §2a.5 only handles a single-level if/else.
- **Severity**: RISK.
- **The cases**:

  **Case 3a — Nested if inside an if**:
  ```
  int x = 0
  if (a) {
      if (b) {
          readonly x
      } else {
          readonly x
      }
      // intersection inside the outer if → x readonly here
  } else {
      readonly x
  }
  // outer intersection: if-branch promoted (after inner intersection), else promoted → x readonly
  x = 1   // should be ERROR
  ```
  This needs the verifier to *propagate* the post-inner-if state up to be visible at the outer if's bottom-of-branch. The spec doesn't say how. The `SymbolTable.snapshotReadonlyState()` API in §2c shows you can read the state, but the verifier algorithm in §2d (lines 1518–1556) only snapshots at "the end of the if branch" — what *is* the end? Is the inner if's join state already merged into the outer branch's snapshot? Probably yes, but the code sketch doesn't make this explicit.

  **Case 3b — Return-in-only-one-elif**:
  ```
  int x = 0
  if (a) {
      readonly x
  } elif (b) {
      return 0
  } elif (c) {
      readonly x
  } else {
      // nothing
  }
  // What is x's state here?
  ```
  Three reaching predecessors: `a` (readonly), `c` (readonly), `else` (mutable). Intersection: mutable. The `b` branch is terminating, ignored.

  This is correct under the designer's rule but the spec only worked one elif example (Snippet 5).

  **Case 3c — Mixed terminating branches**:
  ```
  int x = 0
  if (a) {
      readonly x
  } else if (b) {
      readonly x
      return 0
  }
  // No else.
  // Reaching predecessors: the (a) branch and the *fall-through* (b is false).
  // Fall-through: x is mutable.
  // Intersection: mutable.
  x = 1   // accepted?
  ```
  The implicit fall-through has to count as a predecessor. The designer's spec doesn't mention it. **This is the easiest mistake to make** because "no else branch" is the unstated case.

  **Case 3d — `if` with no `else` and no fall-through (`return` ends the if-branch)**:
  ```
  int x = 0
  if (a) {
      readonly x
      return 0
  }
  // Reaching predecessors: only the fall-through (a is false).
  // Fall-through: x is mutable.
  x = 1   // accepted.
  ```
  This is correct, but again the spec doesn't lay it out.

- **Suggested fix**: replace the spec with a *single* general rule: the post-join state is the intersection of `readonlyAtBranchExit(b)` over all branches `b` that *fall through* to the join (i.e., don't end in `return` / `panic` / `unreachable`). The fall-through edge from "no else" or "no matching elif" is one of those branches, contributing the entry state to the intersection. State this once and exhaustively rather than case-by-case. The verifier code in §2d already nearly says this — the spec text needs to catch up.

---

### F4 — Loops: the per-iteration freeze rule reverses programmer intuition in real loop patterns.

- **Section reference**: 03 §2a.8 (lines 1167–1209), Snippet 6.
- **Severity**: RISK.
- **What the designer claims**: "the freeze applies for the rest of the current iteration's body" (line 1177). Justification (line 1205): "Persisting across iterations would also be a defensible choice, but it makes `readonly x` inside a loop almost always wrong (because the second iteration would hit the same statement on a frozen binding and... what? Re-freeze? No-op?)."
- **The flaw in the justification**: "re-freeze a frozen binding" is *not* a problem the designer can wave away — the designer's own §2c (line 1394–1397) makes it an *error*: `if (info.isReadonly) return VerificationResult(false, "Binding '$name' is already readonly")`. So the designer's own implementation treats double-freeze as a compile error, which means under the per-iteration rule, the verifier walks the body *once* and accepts the `readonly x` once; the rule is internally consistent. But that consistency is only because the verifier walks the body in *static* form (one walk per body); the actual *dynamic* behavior is "the second iteration's readonly fires on an already-frozen binding." The user, reading the spec, may not realize the verifier is doing a single static walk.
- **The accumulator pattern (real failure)**:
  ```
  int total = 0
  for (n in numbers) {
      total = total + n
      readonly currentSum := total      // pin the running total at this iteration
      audit(currentSum)
      // ... more work ...
  }
  ```
  Here `currentSum` is *declared* with `readonly` inside the loop body (Form A on a fresh binding) — fine. But what if the user writes:
  ```
  int currentSum = 0
  for (n in numbers) {
      currentSum = currentSum + n
      readonly currentSum            // FORM B
      audit(currentSum)
  }
  ```
  Under the spec, this works on iteration 1 but on iteration 2: the loop-back-edge intersects "mutable (entry)" with "readonly (bottom)" → mutable. So iteration 2 enters with `currentSum` mutable, the first line writes (allowed), the `readonly currentSum` re-freezes (allowed because not yet frozen this iteration). All consistent.
- **The real surprise** — `break` and `continue` aren't in the grammar yet (audit line 48), but they will be. The spec doesn't address how they interact with the loop-back intersection. Specifically, a `continue` *skips to the loop-back*; should the binding state at the continue site participate in the intersection? Yes, but the spec doesn't say. When P12+ adds `break`/`continue`, the spec needs revisiting — flag this now.
- **Comparison to Kotlin's smart-cast loop semantics**: Kotlin smart-casts inside loops are *invalidated* at the loop-back edge (because the loop body might reassign). Same rule, same conclusion. The designer's rule matches Kotlin's intuition — but neither Kotlin nor TypeScript treats `readonly` (as a state) the same way they treat narrowing (as a type). The designer is borrowing the right rule from the wrong feature.
- **Suggested fix**: keep the per-iteration semantics. Explicitly call out `break`/`continue` as future work (NOT YET RESERVED). Add a worked snippet showing the accumulator pattern with `readonly` *inside* the body declaring a fresh binding (Form A), which is the natural ergonomic pattern.

---

### F5 — Aliasing footgun: the binding-only freeze documentation is honest but the error messages aren't designed.

- **Section reference**: 03 §2a.6 (lines 1123–1146), Snippet 8 (lines 1755–1767).
- **Severity**: RISK.
- **What the designer claims**: "Option 3 is *honest*: it freezes what we can statically guarantee... The error message can be explicit: '`readonly` freezes the binding `a`, not the underlying record. Use `imm record` (Tier 2) for transitive immutability.'" (lines 1141–1142).
- **The problem**: the error message *describes a feature that does not exist*. `imm record` is Tier 2 (line 1146). When the user encounters this in v1 — say two years before Tier 2 lands — the message hints at a workaround they cannot use. Worse, by the time `imm record` lands, the spec for it may diverge and the suggested fix may be wrong.
- **What other languages do**:
  - Rust: doesn't have this problem; the borrow checker tracks aliases. So Rust's analogue is "this aliasing pattern is a borrow-checker error", and the diagnostic points at the alias not the binding.
  - Pony: capabilities make `b = a` itself rejected unless `a` is `iso` (transferred uniquely) or `val` (shareable immutable). So Pony's diagnostic happens at the *alias creation*, not at the mutation. The designer's spec doesn't have this; an alias is created freely.
  - TypeScript: `readonly` is a type property, not a binding property; aliasing through a non-readonly type evades the check. TS users *frequently* trip on this. (This is a known TS footgun.)
- **The footgun scenario** (the user's first encounter):
  ```
  record Box { int v }
  Box a = Box::new(1)
  Box b = a
  readonly a
  b.v = 5      // OK — surprising
  println(a.v)  // prints 5
  ```
  The user types `readonly a` thinking they made the record immutable. They didn't. The next print is a write done by a separate name. Every modern reader will read `readonly a` as "lock a, including what a refers to" because *every other readonly system in the wild* (TS `readonly` on object property types, JS `Object.freeze`, Rust `let` without `mut`) is at least *appears to be* about the object.
- **What's missing from the spec**: a *positive* educational example that *demonstrates the right mental model*. Snippet 8 just says "this is OK" — it doesn't say *why* and doesn't tell the reader how to actually achieve what they probably wanted.
- **Suggested fix**: rewrite Snippet 8 as a "common-pitfall" callout. Add a paragraph at the top of §2a.6 that says "If you want object-level immutability, today you must: (a) avoid aliasing the record, or (b) use defensive copies via `Box::new(a.v)`. Neither is great. Tier 2 will add `imm record`." Drop the error-message wording that references unimplemented features.

---

### F6 — "Effectively novel" claim: Rust's shadowing is more equivalent than the design admits.

- **Section reference**: 02 §3 (lines 393–404), 03 §0 (line 20), 04 R5 (lines 364–367).
- **Severity**: MINOR — this is a marketing/positioning challenge, not a correctness one.
- **What the researcher claims**: "The closest analogs are Java's 'effectively final'... and Rust's pattern of shadowing/rebinding (workaround, not a feature)" (02 line 15). "Form B is a Waterfall-only compile-time concept" (03 line 20).
- **What Rust shadowing actually does**:
  ```rust
  let mut x = compute();
  do_stuff(&mut x);
  let x = x;        // shadow with immutable binding
  // x = 5;         // ERROR
  ```
  This is *not just an idiom* — it's the textbook Rust pattern, taught in the official book (cited at 02 line 404). It's a one-line construct that achieves the same outcome as Form B with no language extension. Pony's `recover` is also block-scoped — and `recover { let val_x = ref_x; val_x }` is one line away from Form B.
- **The Mike-Test implication**: a Rust programmer encountering Waterfall reads `readonly x` and thinks "oh, like `let x = x`". A C# programmer thinks "oh, like `readonly` on a local that was proposed but not adopted." Neither reaction is "wow, novel." The novelty is real but *narrow*: it's a one-line construct that elides the shadowing-and-rebinding ceremony. That's worth something for ergonomics — it's not a category-creating differentiator.
- **The downstream effect**: 04 §6 "elevator pitch" (line 449) doesn't lead with `readonly`. Good. But the strategist also has R5: "the 'novel readonly' feature doesn't pan out" — and the mitigation is "don't market it as headline until P12 confirms" (line 366). This is the right mitigation, but R5 is *undersized as a risk*. The risk isn't "the feature is bad"; the risk is "the team over-invests because they believe it's a moat when it's actually a small ergonomic improvement."
- **Suggested fix**: in 03 §0 and §2, downgrade the "headline differentiator" framing to "ergonomic improvement over Rust's shadowing idiom." Anchor the claim more carefully. Specifically remove "Form B is a Waterfall-only compile-time concept" from §0 line 20 — it isn't; it's a one-line shorthand for what Rust does in two lines.

---

### F7 — Form B optimization opportunity (un-optimized `let` vs. `const`).

- **Section reference**: 03 §2e (lines 1580–1613).
- **Severity**: MINOR.
- **What the designer claims**: "emit nothing for Form B in every backend… The Waterfall verifier enforces that subsequent writes are rejected; the JS runtime never sees them" (line 1613).
- **The missed optimization**: if `readonly x` fires *immediately* after the declaration without any write between them, the JS backend could emit `const x = ...` instead of `let x = ...; /* readonly */`. Worth checking? Probably not — adding flow-sensitive backend logic for a cosmetic improvement is poor cost/benefit. But the spec should at least note it as out-of-scope: "we do not retro-actively rewrite the declaration site even when it would be safe to do so; this keeps the verifier and codegen independent."
- **The deeper question**: is the emitted JS *honest*? A user inspecting the JS sees `let x = 0; x = 1; doSomething(x);` and concludes `x` is mutable in JS. The Waterfall spec says `x` is `readonly` from line 3 forward. The two are not in conflict (verifier-only), but the JS output is *less safe than the Waterfall source*. Source maps (Tier 2 per 1.11) help; until then, the JS output is misleading.
- **Suggested fix**: when emitting Form B, prepend a `// readonly: x is frozen from here` comment in JS / Python / C output. This costs nothing and makes the output self-documenting. The designer rejected this in §2e (line 1589 "preferable for debugging but adds noise"), then reversed in §2e (line 1633 "easier to trace at this stage"), then re-reversed (line 1637 "Pick: emit nothing"). The spec contradicts itself within five paragraphs. Resolve: emit the comment by default in v1, remove in v2 once source maps land.

---

### F8 — `readonly` as a multi-promote: spec is silent on a common edge case.

- **Section reference**: 03 §2c (lines 1394–1397), Snippet 11.
- **Severity**: MINOR.
- **What the spec says**: `if (info.isReadonly) return VerificationResult(false, "Binding '$name' is already readonly")`.
- **The case it misses**: redundant double-promote in a single linear flow:
  ```
  int x = 0
  readonly x
  readonly x    // error: already readonly
  ```
  This is an error in the spec. Fine. But:
  ```
  int x = 0
  if (a) {
      readonly x
  }
  if (b) {
      readonly x      // Is x readonly here? Depends on the path. The verifier may or may not have promoted it.
  }
  ```
  In the verifier's static walk, *each branch* of each `if` is a separate scope. The first `readonly x` mutates the parent scope's binding (per `markReadonly` walking up). But after the first `if` ends, the intersection rule applies — since the else (implicit) branch didn't promote, the parent binding is left mutable. The second `readonly x` then fires on a mutable binding, fine.
  - But: what if the verifier's intersection-rule pass restores the binding to its pre-if state to compute the join? Then `markReadonly` *inside* the if-branch may have mutated the parent and not be reversible. The spec in §2d (lines 1539–1556) says "computeReadonlyIntersection" but doesn't explain how the parent-scope mutation is *undone* if the intersection says "stay mutable."
- **The implementation hazard**: `SymbolTable.markReadonly` (§2c line 1455) walks up the parent chain and mutates. This is *destructive*. Then the join computation in §2d wants to "snapshot the readonly state" and "compute intersection." If the snapshot is taken *after* the body verified (and after markReadonly fired), the parent scope's binding is already promoted. The intersection-says-mutable conclusion can't roll it back.
- **The fix the designer needs**: either
  - (a) `markReadonly` inside a branch is *scoped to the branch's child symbol table* (not destructive on the parent), and the join explicitly *commits* the intersection up to the parent; or
  - (b) the verifier snapshots-then-restores the parent on entry to each branch, computes per-branch effect, then commits the intersection.
- **Suggested fix**: spec out the symbol table's transactional semantics for branch verification. The code sketch in §2c+§2d as written *will not implement the spec correctly* because of this destructive-mutation issue. The implementer will hit it and either rework the API or get the rule wrong.

---

### F9 — Subfield freezing: the grammar deferral is fine, but `readonly` interaction with the *future* record system is unspecified.

- **Section reference**: 03 §2a.7 (lines 1149–1164).
- **Severity**: MINOR.
- **What the spec says**: subfield `readonly x.field` is a syntax error in v1, planned for Tier 2.
- **The missing detail**: when Tier 2 adds records (§1.1), what does `readonly point` mean for `Point { dec x; dec y }`? Under the spec's binding-only semantics, it means "the binding `point` cannot be re-assigned to another `Point`, but `point.x = 5` is fine." That's *opposite* the intuition for value-type-like records.
- **The interaction with the C backend's structs**: the C backend will emit `Point point = {3, 4};` and the user-readable semantics is "this is a struct; copying it copies values; mutating fields mutates the struct in place." Under §2a.6 binding-only, this struct's `point.x = 5` is *still legal even after `readonly point`*. C lets you `const Point p = ...` but then `p.x = ...` is a compile error in C. Waterfall says yes; emitted C says no. Inconsistency between Waterfall semantics and C backend semantics.
- **Counter**: maybe Waterfall should emit `const Point` for records under `readonly`. But then the design changes per-type (records get C `const`, scalars get C `const`, arrays get… what?). Either ship simple binding-only and break the C backend's semantic alignment, or do something type-aware (`readonly` on records means "all subfields frozen" — i.e., transitive at the type level for records).
- **Suggested fix**: explicitly say in §2a.6 that for *future* record types, `readonly` is *still* binding-only (matches the spec) but the C backend can emit `const Point` because in C, structs-as-values copy on assignment. JS / Python don't get the same compile-time benefit (objects are by-reference). Call this divergence out *now*, before users build mental models.

---

### F10 — Grammar: introducing `READONLY` as both a modifier *and* a statement-leading keyword creates a subtle ambiguity.

- **Section reference**: 03 §2b (lines 1322–1364).
- **Severity**: RISK.
- **The proposed grammar additions**:
  - Lexer: `READONLY: 'readonly';`
  - Parser: `modifier : ... | READONLY ;` (modifier in declarations)
  - Parser: `readonlyPromotion : READONLY name=ID NEWLINE+ ;` (statement)
- **The ambiguity**: in the current `statement` rule (line 19-29), the first three alternatives are
  ```
  | typedVariableDeclarationAndAssignment    // modifier* type name=ID EQUALS ...
  | untypedVariableDeclarationAndAssignment  // modifier* name=ID COLON_EQUALS ...
  | variableAssignment                       // name=ID op=...
  ```
  Adding `readonlyPromotion : READONLY name=ID NEWLINE+` introduces a shift-reduce situation at `readonly foo`:
  - Is `readonly` the start of `typedVariableDeclarationAndAssignment` (so `foo` is a `type`)? — possible because `type : QUESTION_MARK? ID (L_BRACKET R_BRACKET)?` makes any `ID` a type. So `readonly Foo bar = 1` is `modifier=readonly type=Foo name=bar = 1`.
  - Is `readonly` the start of `untypedVariableDeclarationAndAssignment`? — `modifier* name=ID COLON_EQUALS` means `readonly foo := 1` parses as modifier=readonly name=foo := 1.
  - Is `readonly` the start of `readonlyPromotion`? — `READONLY name=ID NEWLINE+` means `readonly foo\n` parses here.

  ANTLR's predictive parser will look ahead to disambiguate: at `readonly foo NEWLINE`, the third alternative wins; at `readonly foo BAR EQUALS`, the first wins; at `readonly foo COLON_EQUALS`, the second wins. This *probably* works, but it's not free — ANTLR's adaptive lookahead handles it, but the grammar's intent becomes much harder to read.
- **The worse ambiguity** — empty array as declaration vs promotion:
  ```
  readonly int[]      // syntax error? or "promote `int[]`-named binding"?
  ```
  `int` is a primitive. `int[]` is a type. As a statement-leading promotion, `readonly int[]` would parse as `READONLY name=int (L_BRACKET R_BRACKET …)` which fails the `readonlyPromotion` rule because the rule wants `NEWLINE+` immediately after the ID. So this *fails to parse* — good. But the error message will be "expected NEWLINE, got `[`", which is opaque.
- **Confirmed by reading the grammar**: in the current `WaterfallParser.g4`, the production for `untypedVariableDeclarationAndAssignment` (line 80) is `modifier* name=ID COLON_EQUALS expression NEWLINE+`. ANTLR's adaptive prediction handles `modifier* name=ID ...` lookahead fine. The new production `readonlyPromotion: READONLY name=ID NEWLINE+` is *distinguishable* by the absence of `COLON_EQUALS` or `EQUALS` or `type` after the ID. But: `readonly x NEWLINE NEWLINE` versus `readonly x = 3 NEWLINE`. At the lookahead point of `NEWLINE`, the grammar commits to `readonlyPromotion`. At `=`, it commits to `variableAssignment` — but wait, `variableAssignment` doesn't allow a leading modifier (line 76: `name=ID op=...`). So `readonly x = 3 NEWLINE` actually has no valid parse — it's not a typedVariableDeclarationAndAssignment (no type), not an untypedVariableDeclarationAndAssignment (uses `=` not `:=`), and not a variableAssignment (has a leading modifier).
- **The trap**: a user writes `readonly x = 5` to declare an immutable `x`. Today (with `const`), this works: `const int x = 5` is fine, `const x := 5` is fine, but `const x = 5` is a syntax error (missing type). The user with `readonly` does the same: `readonly x = 5` is a syntax error. The error message they see: "expected NEWLINE, got `=`" — opaque. The fix the user actually wants is `readonly int x = 5` or `readonly x := 5`. The spec doesn't include this diagnostic case.
- **Suggested fix**: add an explicit "you meant to declare or promote" diagnostic when `readonly ID = expr` or `readonly ID := expr` is parsed (lookahead can detect this and produce a better error). Also flag in the spec that the grammar's new keyword `readonly` is *contextual*: legal as both modifier and statement-prefix. Document that as a feature with worked grammar-trace examples.

---

### F11 — Bundling `const` / `imm` deprecation with `readonly` is unnecessary churn.

- **Section reference**: 03 §2a.12 (lines 1297–1316), §2g (lines 1825–1838).
- **Severity**: MINOR.
- **What the designer claims**: deprecate `const` and `imm`, unify on `readonly`. Justification: "one keyword is cleaner than three."
- **The empirical state**: `grep -rn "\\bimm\\b"` across the repo finds **zero** uses of `imm` in any `.wf` file (tests, examples, or anywhere). `const` is used in exactly **3 example files** (`DuplicateDeclarationsModule.wf:2`, `DuplicateVariableDeclarationsModule.wf:3`, `VariablesAndFunctionsModule.wf:2`). Total user-facing impact of removing both: tiny.
- **The argument *for* deprecation**: cleaner headline pitch; fewer ways to do the same thing.
- **The argument *against* deprecation (made nowhere in the spec)**:
  - `const` carries clear meaning across JS, C, C#, Rust. It's the *most learnable* name.
  - `readonly` is *less* learnable for the declaration-time case. TypeScript uses `readonly` on *type modifiers* (`readonly string[]`) and *property modifiers* (`readonly x: T`), not on *binding modifiers*. A TS developer reads `readonly int x = 4` and wonders "is this readonly the type-level thing or the value-level thing?"
  - Renaming three keywords' meaning into one obscures Form A's lineage. Form A is just `const` (the well-known concept); calling it `readonly` is bait-and-switch for the marketing pitch.
  - The deprecation pathway adds friction (lint warnings, then a v2 break) for negligible value (3 places to update by hand).
- **The hidden cost**: every Tier 2/3 feature that lands becomes one more dimension of "what does `readonly` mean here?" because the keyword is overloaded. Adding `imm record` (Tier 2 §1.8 tension) is awkward when `readonly` is the binding modifier — should it be `readonly record` instead? The designer doesn't notice this collision.
- **Suggested fix**: keep `const` as the declaration-time keyword. Keep `readonly` for the statement-form (Form B) only. Two keywords; one concept per syntactic position. Drop `imm` (genuinely unused). This is *less* opinionated than the current spec and avoids confusion with TypeScript's `readonly`.

---

### F12 — `Result<T, E>` is special-cased but the special case is under-specified.

- **Section reference**: 03 §1.6 (lines 619–706), §1.4 tensions.
- **Severity**: RISK.
- **What the spec says**: "`Result<T, E>` is a built-in special-cased generic union for v1. The compiler hardcodes its representation." (line 703)
- **The missing pieces**:
  - What is the *type-system status* of `Result<T, E>` in v1? Without generic unions (Tier 2), how does the user write the type? Per the syntax in §1.6: `func parseInt(char[] s) returns Result<int, char[]>`. So the user *can write* `Result<int, char[]>` but cannot declare `union MyResult<T, E>`. That asymmetry is uncomfortable.
  - What happens when the user wants `Result<int, MyError>` where `MyError` is a non-generic union of error variants? The spec doesn't say. Probably fine because `MyError` is a regular union (Tier 1), but the special-case-generic-union spec needs to spell out which type-args are allowed.
  - Lowering for C in §1.6 says `Result_int_charP` (line 694). What about `Result_dec_MyError`? Does the C backend's monomorphizer instantiate per-pair? If so, the audit's `cType` (CBackend.kt:71-83) has to learn about user unions — but generic *records* aren't until Tier 2. The C backend's monomorphization will need to handle the *generic-union special case* before generic records exist.
- **The risk**: `Result` is on the critical path (§1.6, T1, used everywhere) but its implementation depends on partial generic support that hasn't been designed. The spec waves "the special case dissolves once generics land" — but that's deferring the design.
- **Suggested fix**: pin down the v1 special case. Either (a) `Result<T, E>` only allows scalar `T` and scalar `E` (no records, no nested generics) — minimal but sufficient for the use cases shown; or (b) `Result<T, E>` is fully generic from day 1, which means implementing generic unions *now* and dropping the "Tier 2" framing. Option (a) is less impressive but tractable. Option (b) is more honest but commits more scope to T1.

---

### F13 — `@external` doesn't model side effects.

- **Section reference**: 03 §1.9 (lines 762–812), §4 (lines 1883–1964).
- **Severity**: RISK.
- **What the spec says**: `@external(js, "Math.sqrt")` etc. The function has "no Waterfall body" when an `@external` exists.
- **The hidden assumption**: that target externals are *pure*. `Math.sqrt` is. `setTimeout` (§4 worked example) isn't — it has side effects on the runtime. The spec treats both identically.
- **The downstream issue**: P12 ships `Result<T, E>` for error handling. A Waterfall function that does I/O — say `read_file` — would naturally return `Result<string, IOError>`. But `@external(js, "fs.readFileSync")` doesn't return a Result; it throws or panics. The spec doesn't say how to bridge.
- **Comparison to Gleam**: Gleam's `@external` *does* model side effects, because Gleam tracks purity *via the type system*. Functions returning `Effect(X)` (or similar) compose differently. The strategist's research (02 §2.15) notes Gleam's "expression-level target tracking" — but the *type-level effect tracking* is a separate, important Gleam feature that the design doc didn't borrow.
- **The risk**: shipping `@external` without an effects story means every FFI'd function is a black box. The Waterfall type system says "this returns a `dec`" but the user has to know "this might throw," which it can't statically know.
- **Suggested fix**: in v1, declare every `@external` function as "potentially side-effecting" implicitly. Document this. Don't promise pure-FFI semantics until a Tier 3 effect tracking story lands. Add a worked snippet showing what happens when an `@external` function throws in JS — does Waterfall propagate? Crash? The spec is silent.

---

### F14 — `Mod::fn(x)` becomes a verification error post-import-system, which the spec calls out but undersizes.

- **Section reference**: 03 §1.5 tensions (lines 600–605); strategy 04 P14.
- **Severity**: MINOR.
- **What the spec says**: "What does the existing `Mod::fn` call resolve to when `Mod` was not imported? A verification error. This is the first time the verifier rejects a previously-accepted program — but the program never *really* worked." (lines 604–605)
- **The empirical state**: I confirmed by reading the codebase that `Mod::fn` *does* in fact "work" in JS / Python today, because the target language's name resolution handles it. The README quick-start examples likely use it. Audit §6 surprise #10 says "cross-module references aren't resolved at compile time, only at emit time." So programs that work today *will break* once imports land.
- **The undersized impact**: this is a breaking change to every Waterfall program that uses `Mod::fn`. The strategist's roadmap (P14 line 246) says "JS backend: pick ESM and commit" without flagging that this is *also* a breaking change for users who built on the implicit-target-resolution behavior. The "very few users" defense doesn't cure the principle: a v1 promise of stability has to know what `Mod::fn` means *before* shipping.
- **Suggested fix**: explicitly enumerate the v0→v1 breaking changes in 04. Include: `Mod::fn` requires explicit `import`; `const`/`imm` deprecated (if 11 not adopted); empty-array literal `[]` (still missing); etc. This is a list, not a paragraph.

---

### F15 — Strategy: the teaching niche is plausible but the *path to recruiting users* is hand-wavy.

- **Section reference**: 04 §2 Candidate 2 (lines 78–87), §6 production-user section (lines 439–441).
- **Severity**: RISK.
- **What the strategist claims**:
  - "Plausible addressable: low tens of thousands" (line 83).
  - "By end of P15, we should have ~2 candidate users we're cultivating relationships with." (line 440)
- **The hand-waviness**:
  - "Low tens of thousands" is sourced from r/ProgrammingLanguages (36k members) and "CS departments offer compilers courses to thousands of students annually." But *how many of those would actually try a teaching language*? Not 36k. Crafting Interpreters has sold *low five figures* of copies over years — that's the *book* market, not the language market.
  - "Compete with prose, not code" (line 124) is honest. But prose-competition means we need a *teaching artifact*, not just a language. The roadmap puts a blog post at month 4 (R3 trip-wire, line 357) — that's exactly one artifact. Crafting Interpreters succeeded because it's a book-length teaching artifact. A blog post is not a book.
- **What other teaching-positioned languages did**:
  - Pyret: had a textbook (HtDP-aligned) and was used in Brown's CS course. It survived as a course tool, not a broader language.
  - Racket: heavily invested in HtDP and the educator community for decades. Still not mainstream beyond its niche.
  - LOX: companion-to-a-book; not a standalone language.
  - None of these are "Tier B" in the sense the strategist defines (1,500+ stars, multiple production users, recognized in surveys). Teaching positioning *correlates with* small reach, not sustained niche reach.
- **The missing piece**: the strategist's recruiting plan is "the people most likely to ship Waterfall in production are people who learned to write a compiler with it" (line 440). This is plausible but unsupported by evidence — most people who learn from a teaching artifact don't ship in production using the same tool. They learn the *concept*, then use a different tool for production.
- **Suggested fix**: be more honest about the realistic ceiling for a teaching-positioned language. Set the 5y star target to 1,000–1,500 (not the current "≥1,500"); set the 5y production-user count to 1 (not omitted); explicitly call out that "teaching positioning" likely converts to *zero* production users without a Crafting-Interpreters-class artifact alongside the language. Then plan the artifact: a teaching book or a series of well-edited blog posts, *not* a marketing afterthought.

---

### F16 — Strategy: P10 (foundation refactor) is correctly prioritized but its *cost is underestimated*.

- **Section reference**: 04 P10 (lines 145–164).
- **Severity**: RISK.
- **What the strategist claims**: "Effort: Quarter (≈3 months of evenings). This is the most expensive and least visible work in the roadmap."
- **The scope of P10**:
  - Replace `Any?` with `SymbolInfo` (mechanical refactor across ~10 callsites — a week).
  - Public `SymbolTable.lookup` (trivial).
  - "Introduce an `Ir/` package with a typed AST distinct from the ANTLR-derived `*Data`" (closes D1).
  - "Move `verify()` into a dedicated verifier package; `*Data` classes lose the verify method" (closes D3).
- **The unspoken work in introducing an IR**:
  - Re-implementing every `*Data → IR` lowering (audit lists ~20 statement / expression kinds).
  - Re-implementing every backend's `emit*` to consume IR instead of `*Data` (4 backends × ~20 emit methods = 80 method rewrites).
  - Re-implementing the verifier to walk IR (not `*Data.verify` recursively).
  - Re-running all 4 × N golden tests and confirming byte-identical output.
- **Empirical baseline**: the codebase audit (01 §6 fact 16) confirms the project's Phase 9b/9c migrated Java → Kotlin and that took *two phases* of dedicated work. P10's work is *larger* — same code touched, plus the IR introduction, plus the verifier separation. A side-project pace of 1 quarter is optimistic by a factor of 2-3x.
- **Why this matters strategically**: P10 is foundational; if it takes 9 months instead of 3, every downstream phase slides. The 3-year-to-v1.0 target becomes 4-5 years. The motivational risk (R7) compounds: the longer P10 takes, the more "least visible work" weighs on the maintainer's morale.
- **The escape hatch**: do P10 *incrementally*. The strategist's roadmap implies a single quarter-long phase; reality favors slicing P10 into "redesign SymbolInfo first (smallest change, biggest enabler)," then "add IR for just one statement kind end-to-end," then propagate. This converts a single big-bang refactor into a series of smaller "still ships" changes.
- **Suggested fix**: re-scope P10 as a *meta-phase* that runs in parallel with P11-P12 features, instead of blocking them. Land SymbolInfo first (week 1-2). Land public `lookup` (week 1). Add IR incrementally as features land, with each feature touching one slice of IR. The Strategist's R4 trip-wire ("1.5x effort estimate") will fire on P10 as-currently-scoped; pre-empt by re-scoping.

---

### F17 — Strategy: 3-year-to-v1.0 timeline is unrealistic for evenings-and-weekends pace.

- **Section reference**: 04 §3 (lines 318–333), Q2 (lines 467–469).
- **Severity**: RISK.
- **The math**: 7 quarters of "effort" — but the strategist herself says "side-project pace → ~3 calendar years." Side-project pace is typically *quarter-of-full-time* (8-10 hours/week is typical for evenings + weekends). 7 quarters of full-time effort = 28 quarters of side-project effort = 7 years.
- **Even adjusting downward**: 7 *side-project* quarters means ~2 years. But P13 alone is "2 quarters" of effort (line 230), and includes building an LSP, a package manager, a published spec, a VS Code extension, and friendly error messages. The strategist's own R4 (line 360) warns about feature creep — but P13 *is* feature creep. The actual P13 effort is closer to 4-6 quarters at side-project pace.
- **Comparison to peer indie projects**:
  - Gleam: started 2016, hit v1.0 in 2024. **8 years.** Solo at first, with paid maintainer time later.
  - Crystal: started 2014, v1.0 in 2021. **7 years.** Multiple contributors.
  - Nim: started 2008, v1.0 in 2019. **11 years.** Multiple contributors.
  - Zig: started 2015, v1.0 still pending in 2026. **11+ years.** Multiple contributors and corporate sponsorship.
- **What this means**: an indie language hitting v1.0 in 3 years is *exceptional*, not typical. The strategist's plan to do it on evenings-and-weekends is optimistic.
- **Why the strategist's plan looks reasonable**: phase budgets are estimated in "effort," not calendar time. The "calendar 3 years" claim hand-waves the conversion. Honest math says 5-7 years.
- **Suggested fix**: target *v0.x* in 3 years and *v1.0* in 5-7 years. Define what v0.x means concretely — probably "all T1 features in spec, all working on JS + Python, C as best-effort, no LSP yet." That's a recoverable milestone; "v1.0 in 3 years" sets up a perceived failure if Year 3 lands with the LSP unfinished.

---

### F18 — Strategy: R1 (bus factor) mitigation depends on attracting a contributor that the niche doesn't supply.

- **Section reference**: 04 R1 (lines 344–348), §6.
- **Severity**: RISK.
- **What the strategist claims**: "Aim for second contributor at P12 (sum types is a fun feature to attract a contributor on)." (line 347)
- **The structural problem**: the teaching niche attracts *students*, not *contributors*. Students who learn from a teaching artifact rarely contribute upstream because (a) they're learning the basics, not implementing them; (b) they move on once they've learned. The two demographics most likely to contribute to a small language are:
  1. PL hobbyists (small audience; orthogonal to teaching).
  2. Working developers who use the language (zero, given the teaching niche).
- **Elm's cautionary tale, retold**: Elm has a fanatic user base but stagnant maintainership because the *user base is a niche*, not because no one cares. The same dynamic would apply to Waterfall under the teaching niche: students care; nobody contributes.
- **Comparison to peers**:
  - Gleam: attracted multiple contributors *during the language design phase*, before stable. The teaching pitch was not the draw — the *language design itself* (BEAM + friendly errors) was.
  - Crystal: same pattern. The language design (Ruby + types) attracted contributors.
  - Pyret (teaching-positioned): never attracted significant external contributors despite educational use.
- **What this means**: the niche choice (teaching) is *anti-correlated* with the highest-priority risk mitigation (attracting contributors). The strategist's plan needs either a different niche-for-contributors story or a different bus-factor mitigation.
- **Suggested fix**: separate "who is the language for?" from "who do we recruit as contributors?" Teaching can be the user-facing pitch, but contributor recruitment needs a separate hook: "interesting PL design questions" (the `readonly` work, even if it's not headline-novel, *is* PL-design-interesting); "small, readable compiler" (the codebase is genuinely small). The two hooks aren't the same, and the strategy doc treats them as one.

---

### F19 — Strategy: AI-assisted development changes the indie-language cost equation, but the strategy doesn't account for it.

- **Section reference**: 04 — not addressed.
- **Severity**: MINOR.
- **The 2026 context**: AI coding assistants (Claude, Copilot, Cursor) have changed the side-project economics. A solo maintainer in 2026 can ship code at 2-5x the rate of a 2020 solo maintainer. This is a *real shift*, not a marketing claim — every indie PL project active in 2026 is benefiting.
- **The unaddressed effects**:
  - Foundation refactors (P10) are *cheaper* in an AI-assisted world. The mechanical SymbolInfo migration across 10 callsites is a one-evening job, not a one-week job.
  - LSP implementation (P13) is *cheaper*. The boilerplate of LSP4J is exactly what AI is good at.
  - Documentation and spec writing (P13) is *cheaper*. Drafts can be auto-generated and edited.
  - Conversely: the *expected developer experience* has risen. Users in 2026 expect their language to play nicely with AI-assistant editor integrations (LSP + structured types). Without LSP from day 1, users may not engage.
- **Why the strategy doesn't mention this**: blind spot, or scope. The strategy reads as if it were written in 2018.
- **Suggested fix**: add a one-paragraph note in 04 about the AI-assist effect. Likely outcome: P10 and P13 effort estimates come *down* by ~50%, but user expectations for LSP and friendly errors come *up*. The two roughly cancel. But the strategist should be explicit so the maintainer doesn't both (a) over-estimate effort and (b) under-deliver on UX.

---

### F20 — Strategy: the "polyglot teaching" framing leaves money on the table by ignoring AI-assisted code generation as a use case.

- **Section reference**: 04 §2 Candidate 2 (lines 78–86).
- **Severity**: RISK.
- **The unexplored niche**: in 2026, an LLM that writes Waterfall code gets the property "this code emits to JS, Python, and C readably." That's *interesting* for a use case the strategy doesn't mention: prompting an LLM to write code once and getting target-language artifacts. Compared to asking the LLM to write in 3 languages, it's tighter feedback.
- **The market**: AI-generated code is increasingly being shipped to production. A small language whose output is *legible* in three targets is a niche for AI-pipelines where the human verifies one source and gets three targets.
- **The counter-argument**: LLMs are equally good at generating native JS / Python / C. The "write once, get three" pitch is undermined by "the LLM can write three from one English prompt." But: the *consistency* across targets — same semantics, byte-identical-when-deterministic, machine-checkable — is something prompting can't guarantee.
- **Why it's worth mentioning**: the strategist's "polyglot teaching" framing positions the artifact for *humans learning from prose*. The "AI-generated code multitarget" framing positions the artifact for *machines generating code*. They're complementary, not competing. The latter scales differently — if it works at all, it scales fast.
- **Suggested fix**: not necessarily to *change* the primary niche, but to acknowledge in 04 §2 that the multi-target story has *two* downstream uses (humans and AIs), and the teaching positioning addresses one. Don't bet the language on AI-codegen, but don't blind-spot the possibility either.

---

### F21 — Strategy: non-goal NG6 (no concurrency) trades a real audience away.

- **Section reference**: 04 §5 NG6 (lines 400–402).
- **Severity**: MINOR.
- **What the strategist claims**: "No goroutines, no async/await, no STM, no actors in v1."
- **The argument *for* NG6**: scope discipline; concurrency on three different runtimes (JS event loop, Python GIL, C threads) is genuinely hard; the multi-target story would crack under concurrency.
- **The argument *against* NG6 (made nowhere)**: every "modern" language in 02 has *some* concurrency story. Go has goroutines; Swift has structured concurrency; Kotlin has coroutines; Gleam has actors; Roc has effects. A language without *any* concurrency story is read as a teaching tool, not a production tool. The teaching niche is fine with this; the "Tier B legitimate language" framing is *not* fine with this.
- **The cost of NG6**: any "this is for production use" candidate user looks at "no async/await in 2026" and walks away. That's compatible with the teaching niche; less compatible with the 5y-milestone of "≥1 documented production user."
- **Suggested fix**: NG6 should be more precise: "no concurrency in v1; we will adopt JS-Promise / Python-asyncio interop via FFI by v1.x; native concurrency is Tier 3." This preserves the scope-discipline argument while leaving a path open. The current NG6 is too absolute.

---

### F22 — Completeness: the design omits a strings-as-immutable-by-default question.

- **Section reference**: 03 §1.7 (strings as a real type).
- **Severity**: MINOR.
- **The omission**: in Rust, Swift, Kotlin, Gleam, Crystal, Python, and Java, strings are *immutable*. In C, they're mutable (`char *`). The design says "`string` lowers to `char *` in C" but doesn't discuss whether Waterfall strings are immutable.
- **The trap**: if a user writes `string s = "hello"` and then `s = s + " world"`, are they (a) mutating the bytes of `s`, or (b) rebinding `s` to a new string? In modern languages (a) is unavailable; in C (a) is the default if you have a `char *`.
- **Why it matters for `readonly`**: `readonly string s = "hello"` should reject `s = ...` (binding) but what about `s[0] = 'H'`? In the current grammar, `s[0] = 'H'` isn't even expressible (audit row "Array index on LHS"); but the spec will add it eventually.
- **Suggested fix**: declare strings as *value-typed immutable* in v1. Sidesteps the question. Lowers to `char *` in C with the *convention* that the bytes aren't mutated; the C runtime's `Waterfall_str_concat` (§1.13) allocates a new buffer. Document this once.

---

### F23 — Completeness: no story for source positions on errors when emitted code is later debugged.

- **Section reference**: 03 §3 Principle 5, audit D7.
- **Severity**: MINOR.
- **What the spec says**: Diagnostics get rich. Source maps are Tier 2.
- **The gap**: when emitted JS hits a runtime exception (e.g., a `null` deref from a Waterfall null-pointer escape), the user sees a JS stack trace with line numbers in the *emitted* file, not the Waterfall source. Source maps fix this for JS; Python honors line numbers natively; C needs `#line` directives. The design mentions all three (Tier 2) but doesn't say which is required for v1.
- **Why this matters**: a teaching-niche user *wants* to see how Waterfall lowers to JS — they'll be reading both files. But a not-explicitly-teaching user (a Tier B legitimacy-stamped production user, however small) wants Waterfall traces. The roadmap should commit.
- **Suggested fix**: declare in v1 that emitted code includes source-pos comments (`// .wf line 42` at the start of each emitted statement) as a stopgap before source maps. Costs nothing; helpful in teaching too.

---

## The Mike-Test

*Mike is a 28-year-old working developer who picked up TypeScript at his last job and Rust over a recent weekend. He learned of Waterfall in early 2027 via a hacker-news post titled "Waterfall: a small language that compiles to JavaScript, Python, and C — and lets you freeze variables mid-function." He's installed it in 20 minutes and is writing his first non-toy script.*

Mike opens the README. The headline pitch about `readonly` Form B is interesting. He writes:

```
func count(int[] nums) returns int {
    int total = 0
    for (n in nums) {
        total = total + n
    }
    readonly total
    audit(total)
    return total
}
```

It compiles. He inspects the JS output — `let total = 0; ... let total = ...`. He scrolls. The `readonly total` line emitted *nothing*. He thinks "oh, it's verifier-only." He's mildly surprised that the JS output uses `let`, not `const`, but accepts it.

He writes the next function:

```
func updateScore(int s) returns int {
    int score = s
    if (score < 0) {
        score = 0
        readonly score
    }
    score = score * 2
    return score
}
```

It compiles. He stares at it. The `readonly score` inside the if-branch promoted, but the join intersected with the implicit-fall-through-mutable, so the outer `score` is mutable. The `* 2` line runs. He never realizes `readonly` did nothing because the assignment on line 7 went through fine and matched his intent. Form B did not surprise him *here*.

He writes the third function, trying the lambda case:

```
func tally() returns int {
    int sum = 0
    const add := (int n) ==> incrementBy(n)
    add(5)
    add(10)
    return sum
}
```

This doesn't do what he expects — but the failure is in `incrementBy(n)` (which would have to mutate `sum`, which lambdas can't do in v1) — not in `readonly`. He's confused but the confusion is *not about readonly*.

He moves on. The error message catches him out next: he writes `readonly int x = 5; x++` and gets:

```
ParseError: at line 2, col 1: unexpected token `++`
```

Wait, that's the parser failing on `x++` for some unrelated reason. Then he writes `const int x = 5; x = 6` and gets:

```
verification failed
```

No file:line, no quoted source. He has no idea what failed. He checks stderr. `module Test at 3:5: Cannot assign to readonly binding 'x'.` OK — found it. But the workflow is "ctrl-c the stdout, scroll stderr, find the file:line." For one error. **The first thing that frustrates Mike is not `readonly` at all — it's the *diagnostic UX*.** That's audit D5; the strategy puts it in P13.

**The implication**: the design team is laser-focused on `readonly` because the user-lead asked them to be. But the *first user frustration* is the un-friendly error output, which the strategy explicitly puts behind 18 months of foundation work. Mike will bounce before reaching the `readonly` value-add.

(Mike continues for ten minutes, hits another error with similar UX, and switches back to TypeScript "for now.")

---

## What the team got right

Calibrating trust: these are decisions across the design and strategy that I tried to find holes in and could not.

1. **Foundation refactor first (P10 priority).** Despite my F16 critique of the *cost estimate*, the *priority* is correct. Adding T1 features on top of D1/D2/D3 debt would mean re-doing the work twice. The strategist's "closing thought" framing is right.
2. **Binding-only freeze for aliasing (§2a.6).** The honest call. Deep freeze requires aliasing analysis Waterfall isn't ready for; transitive runtime freeze is a JS hack; binding-only is the only choice that's both simple and shippable. The F5 critique is about documentation, not the call itself.
3. **`@external` Gleam-style FFI over Haxe `#if` (§1.9, §4).** Right choice. Function-level granularity composes with the type system; statement-level fragments. The research backs this up specifically.
4. **Rejecting Tier C (04 §1).** The argument that no indie-built transpiled language has reached Tier C is empirically solid (verified against the 02 dataset). Targeting Tier B is the right ceiling.
5. **Dropping enum-and-union to one keyword `union` (§1.2).** Many languages waste a keyword on this distinction. Picking one is principled.
6. **Generics monomorphized for C, erased elsewhere (§1.4).** The right hybrid. The alternative (uniform erasure everywhere) makes the C backend produce inscrutable void-pointer output. The alternative (uniform monomorphization) wastes work for JS/Python.
7. **Pattern matching as expression (§1.3 tensions).** Aligns with Rust/Swift/Kotlin; the alternative (statement-only) would feel dated.

These calls are robust. My disagreements are scoped to *how* the team gets where it's going, not *where* it's going.

---

## Overall assessment

**The approach largely holds up.** One FATAL (F1) is a real soundness issue that — critically — only fires *after* P12 ships multi-statement lambdas; that's the actionable response. The 15 RISKs are mostly spec-clarity and scope/effort calibration issues, not design-defeating problems.

The strategy's *direction* (Tier B + teaching niche + Gleam playbook) is defensible. The strategy's *timeline* (3 years to v1.0 on evenings/weekends) is optimistic by a factor of 2x. The strategy's *contributor story* (R1 mitigation via teaching niche) is internally inconsistent.

The `readonly` design's *novelty* is overstated relative to Rust shadowing (F6); the marketing should anchor on ergonomics, not on category-creation. The *semantics* are mostly right but the *spec text* leaves edge cases undefined (F3, F8, F9). The *implementation sketch* (§2c+§2d) has a destructive-mutation bug that will surface in the first nested-if test.

**The single highest-leverage fix**: address F8 (symbol-table transactional semantics) and F3 (joint-state spec generalization) before implementation begins. Both are spec problems, both are tractable, and both will cause real implementation bugs if not pre-empted.

**The single most important pivot**: rewrite F15 (teaching-niche recruiting plan) with a concrete teaching artifact commitment. Without it, the niche choice is a wish, not a strategy.

---

*End of review.*
