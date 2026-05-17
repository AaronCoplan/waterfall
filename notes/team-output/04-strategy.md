# Waterfall — Long-Term Strategy and Roadmap (Task #4)

Author: strategist
Date: 2026-05-14
Inputs: `01-codebase-audit.md`, `02-pl-landscape.md`, `README.md`, `notes/AUDIT-OPEN-QUESTIONS.md`. Language-designer's `03-language-design.md` not yet available at write time; capability gaps below are derived directly from the audit's Section 1 matrix.

This document is opinionated. The thesis comes first; the reasoning follows.

**Thesis in one sentence** (updated for AI-augmented build cadence + module-reorder + round-3 fixes): Waterfall should aim for the **upper end of the "sustained niche"** tier (Haxe / Gleam / Crystal class), targeting **polyglot library authors writing focused code that needs to ship across runtimes — codecs, parsers, math kernels, ML inference, crypto, ZK, protocol implementations**, by adopting the **Gleam playbook** (LSP, package manager, public spec, friendly errors, expression-level target tracking) with **Gleam-grade language-design taste** — built via **AI-augmented spec-first development** (Klabnik/Carlini playbook) that compresses *technical completeness* to **5–7 months** of weekend/evening work (median ~5–6 months after round-3 absorption of P13 honesty and idiomatic-output polish in P14) while keeping the *adoption-bound* legitimacy milestones on a 1–3 year calendar. Teaching content (the former primary niche) is the **content-marketing vehicle**, not the headline. The single most load-bearing decision is **verification discipline in week 1** — operationalized as a tool-agnostic per-phase triad (property tests / differential oracle / AI-generated adversarial inputs), not a mutation-test slogan.

---

## Section 1 — Define "Legitimate"

The user said "legitimate programming language." That word does work in three different registers, and which one we pick determines every downstream choice. Here are the three I considered, ranked by ambition.

### Tier A — Credible hobby project (low ambition, achievable)
Wikipedia PL list entry; 200–500 GitHub stars; 3–10 outside contributors; used by a handful of people for one-off tasks. Visible on r/ProgrammingLanguages. Maintainer still has full editorial control; no governance load.
- **Concrete shape:** Lobste.rs front page once. A 30-min YouTube tour. A blog post or two. No production user. No conference talk.
- **Time to reach:** 12–24 months from today at the current pace.
- **Honest read:** the codebase is already 60% of the way here in lines-of-code terms. The reason Waterfall is not at Tier A *today* is missing surface area, not depth — no LSP, no spec, no package, no marketing.

### Tier B — Sustained niche language (medium ambition, realistic) — RECOMMENDED, UPPER END
Haxe / Gleam / Crystal / Nim tier. **Aim for the upper end of this tier**, reflecting the more ambitious library-author niche. The target band at v1.0 (legitimacy sense — see split below) is ~1.5k–3k GitHub stars (Gleam was ~5k at v1.0 with vendor-less momentum; Crystal was ~15k at v1.0 after 10 years; Nim was ~9k at v1.0). Library-author niches historically punch above their stars-count for staying power — Rust's `serde` carries enormous mindshare relative to its star count; the same dynamic applies to libraries that become load-bearing in multiple ecosystems.

### v1.0 split into two milestones (new under AI-augmented build cadence)

The empirical evidence on AI-augmented compiler development (`07-ai-augmented-dev-research.md`) collapses *technical completeness* into months — Klabnik shipped Rue's compiler core in **11–14 days** of after-work effort with Claude Code; Anthropic's Carlini built a Linux-compiling C compiler in **2 weeks** with 16 parallel agents; Lambeau shipped Elo in **24 hours** for an expression language. But the evidence equally clearly says *adoption / production-trust / community* milestones do **not** compress proportionally — Bun's Rust port hit 99.8% test pass in 9 days and nobody runs it in production six months later. **Verification design** also doesn't compress; it requires *more* care under AI augmentation, not less.

So Waterfall's v1.0 splits into two distinct milestones:

- **v1.0 technical-complete (target: 3–6 months from project kickoff, median 5–6).** Feature-complete per spec; spec frozen; LSP feature-complete; package manager working; all backends polished; verification suite (property tests, differential tests, fuzzing) in place; "some bugs OK until adoption" is the accepted standard. **No production-user requirement at this gate.** This is the milestone an AI-augmented build can plausibly hit.
- **v1.0 legitimacy (target: 12–24 months from project kickoff).** Adds: **≥3 production library users**, ≥1,500 GitHub stars, ≥50 active community members, ≥1 conference/meetup talk, packages published to npm + PyPI + a vendorable C header working in the wild. This is the milestone the *language ecosystem* hits, and it runs on a calendar that AI cannot compress.

### Calendar milestones (rewritten against AI-augmented evidence)

The previous draft of this section calibrated horizons against no-AI peer baselines (Gleam ~8y, Crystal ~7y, Nim ~11y to v1.0). Under AI augmentation, **technical completeness compresses dramatically; legitimacy and ecosystem do not**. The closest empirical analog is Steve Klabnik's Rue (compiler core in 11–14 days of evenings, ~100K lines, 700+ commits — see `07-ai-augmented-dev-research.md` §1.2); the closest negative analog is Bun's Rust port (technical milestone hit in days, production-trust nowhere in sight six months later — §1.5). Both apply.

- **5–7-month milestone (target window: 2026-10-14 to 2026-12-14):** **v1.0 technical-complete.** P10–P16 complete per the AI-augmented phase plan in §3 (18–28 calendar weeks after round-3 honest recalibration; median ~22). Spec frozen at v0.9 (final v1.0 freeze happens after one round of adversarial review). LSP feature-complete. Package manager working end-to-end. All backends pass the per-phase verification triad (property tests, differential oracle, AI-generated adversarial inputs). C backend output is *runtime-verified*, not just syntax-checked — every example compiles AND links AND runs AND produces the expected stdout (per the P11.5 execution-oracle deliverable). **Idiomatic-output polish included**: JS source maps, TypeScript `.d.ts`, Python docstrings + type stubs, and idiomatic package metadata (per P14 deliverable). First Aaron-authored library (CRC32 or similar — see §6) shipped to npm + PyPI + C header from one Waterfall source as the seed case study, *with idiomatic output that meets 2026 ecosystem expectations*. The widening of the milestone band from "6-month" to "5–7-month" reflects the round-3 honesty on P13 (LSP + friendly errors + stdlib + spec + package manager + VS Code extension at 2–5× compression yields 7–13 calendar weeks, median ~10, not 4–6) and the absorption of Mike-Test-#2 idiomatic-output polish into P14.
- **12-month milestone (2027-05-14):** **v0.x → v1.0 spec frozen; first external library author trying real ports.** ≥250 GitHub stars. The 6-month → 12-month gap is *adoption work*, not implementation work — content marketing, case-study cultivation, community channel building. AI compression doesn't help here.
- **24-month milestone (2028-05-14):** **v1.0 legitimacy hit.** ≥3 documented production library users (each shipping their library to ≥2 of {npm, PyPI, C-header release} via Waterfall); ≥1,500 GitHub stars; ≥50-active-member community; package manager has ≥50 packages of which ≥5 are external. First case-study published with credible before/after diffs.
- **3-year milestone (2029-05-14):** **v1.x maintained, broader community.** ≥3,000 stars; ≥10 production library users; ≥100-member community; annual virtual meet exists; ≥1 conference/meetup talk by someone other than Aaron; surveys / roundups starting to mention Waterfall alongside Gleam/Crystal/Nim.
- **5-year milestone (2031-05-14):** **Recognized small-niche language.** ≥5,000 stars; ≥200-member community; Wikipedia PL page edited by people other than Aaron; ≥1 library originally written in Waterfall that has become *the* canonical implementation of its algorithm in two or more language ecosystems.

The previous draft's 10y horizon is dropped — at AI-augmented build velocity, 10y is too far out to plan around. If we hit the 5y milestone, we're at sustained-niche-language. If we don't, the project's status by then is clearer than any 10y plan can usefully prescribe.

### Tier C — Mainstream-recognized (high ambition, not realistic)
TypeScript / Kotlin / Go / Swift tier. Corporate backing. PL summit appearances. JOBS posted with the language as a requirement. Conferences dedicated to it.
- **Time to reach:** 10+ years, requires either (a) a vendor adoption or (b) a single famous shipped product that depends on Waterfall.
- **Honest read:** Not achievable for a single-maintainer side project. The landscape research showed that **every Tier C transpiled language has vendor backing** (Microsoft/TS, Google/Dart, JetBrains/Kotlin). No indie-built transpiled language has reached Tier C in the modern era. Pursuing Tier C as a goal is a way to fail at Tier B because the work allocation is wrong.

### Recommendation: aim for Tier B. Plan as if Tier A is the floor.

Why Tier B over Tier A:
- Tier A is too easy to reach to be a north star. If we set Tier A as the goal we'll cap at Tier A and the project will feel "done" before it does anything interesting.
- Tier B forces the *legitimacy-bar* investments (LSP, spec, package manager, semantic value-add) the landscape research said are non-negotiable in 2026. Those investments are what distinguish "credible language" from "compiler experiment." Without them, we are in the second category forever.
- Tier B is reachable for indie maintainers — Gleam, Nim, Crystal all got there without vendor backing. That's the existence proof.

Why not Tier C:
- It demands resources Waterfall does not and will not have. The path requires either Microsoft-tier marketing or a one-in-a-thousand killer app. Planning for it means *over-investing* in things that don't help Tier B (e.g., enterprise governance, foundation incorporation, marketing budgets). Better to stay flexible and let unexpected success route us upward if it comes.

The rest of this document assumes Tier B is the goal.

**Restating the success criteria above as testable outcomes** (this is what we measure). The horizons are calibrated against two independent evidence bases: (1) AI-augmented compiler work (Klabnik's Rue at 11–14 days, Carlini's C compiler at 2 weeks — see `07-ai-augmented-dev-research.md`) sets the *technical completeness* clock; (2) peer-language adoption history (Gleam, Crystal, Nim — `02-pl-landscape.md`) sets the *legitimacy / ecosystem* clock. AI compresses the first; nothing compresses the second.

The 24-month / 3-year / 5-year horizons reflect adoption-bound milestones that the AI-augmented build cadence cannot shorten. The 6-month / 12-month milestones reflect technical-completion compression that the AI evidence does support. The 10-year horizon from the previous draft is dropped — at AI-augmented build velocity, the project is either alive at year 5 (and the 10y picture is clear) or it isn't.

---

## Section 2 — Niche Selection

The landscape research surfaced three plausible "Waterfall niches." I'll evaluate them on the same axes, then two more candidates I generated, and recommend one. (**Note:** the original recommendation in this section was Candidate 2 — polyglot teaching language. After Aaron's Q1 answer, the recommendation moved to a new Candidate 5 combining library-author positioning with Gleam-grade language-design taste. The earlier candidate evaluations are preserved below for the audit trail; the new recommendation is in the section titled "Recommendation (revised)".)

### Evaluation framework

For each niche I ask:
- **Who is the user?** (specific persona, not "developers")
- **What pain do they have today?** (a felt pain, not a hypothetical)
- **Why would they switch?** (specifically *to Waterfall* — not to "a language like Waterfall")
- **Realistic addressable population?** (order-of-magnitude estimate)
- **Direct competitors in this niche?**
- **What does winning here look like?**

### Candidate 1 — "The friendly Haxe" (cross-platform game scripting + general transpiler)

- **User:** indie game devs writing tooling, content scripting, or cross-target gameplay logic; also general-purpose "compile to many places" developers.
- **Pain:** Haxe works but feels old; Lua is dynamic; JS is the lowest common denominator. Want types and modern ergonomics with multi-target output.
- **Why switch:** Friendly errors, Gleam-style polish, multi-target.
- **Realistic addressable population:** ~6.9k Haxe stars × 20 years = the entire surface area of this niche. The total addressable market for "small multi-target languages" appears to be measured in low thousands of regular users. *Highly* competitive — Haxe has 20 years of mindshare and Reflaxe extensibility; Gleam has 21k stars and is actively eating the small-language mindshare.
- **Direct competitors:** Haxe (entrenched), Nim (multi-target via C), F# Fable (more targets than Haxe), Roc (platform abstraction).
- **Winning here:** Would require taking share from Haxe. The history of indie projects displacing Haxe in its own niche is empty. Even Gleam, the strongest small-language entrant of the decade, has *not* displaced Haxe in games — they share mindshare but Haxe's commercial production use (Dead Cells) is untouched.
- **My read:** **Crowded and saturated.** This is the obvious niche for Waterfall and probably the wrong choice for exactly that reason.

### Candidate 2 — "The polyglot teaching language" (write once, see how three target families compile it)

- **User:** CS undergrad / bootcamp grad / self-taught developer trying to understand how compilation works; also instructors teaching intro PL or compilers courses; also working developers who learned one stack and want to see how their Python idioms translate to C.
- **Pain:** Compiler courses use toy languages (textbook MiniJava, Tiger) that don't compile to anything you can run. Real-world transpilers (Babel, tsc) are 100k+ LOC and inscrutable. There is **no good middle ground**: a real-but-small compiler whose output across multiple targets you can actually read and compare side by side. Compiler textbooks describe pipelines abstractly; they don't show you what `for(x in xs)` looks like in C vs Python vs JS for the *same input*.
- **Why switch:** Waterfall's defining quirk — **emitting four different targets from one frontend** — is uniquely valuable as a teaching artifact. The golden tests at `compiler/src/test/resources/golden/<target>/` are already the format a student would want.
- **Realistic addressable population:** Hard to size precisely, but the "I want to learn how compilers work" audience is much larger than the "I want to ship a multi-target language" audience. Crafting Interpreters (Bob Nystrom) is a multi-thousand-copy success. r/ProgrammingLanguages has 36k members. CS departments offer compilers courses to thousands of students annually. Plausible addressable: low tens of thousands.
- **Direct competitors:** Crafting Interpreters (book, not a language); lox (toy language from the book — single-target, not maintained); various course languages (MiniJava, Tiger, Cool). **No production-quality multi-target teaching language exists** that I can identify.
- **Winning here:** Tutorial videos, blog posts on PL topics, a "how Waterfall compiles X" series, a textbook-companion chapter. Mentioned in compilers-course reading lists.
- **My read (revised):** **Under-served as content, but the wrong primary niche.** The "compare how four targets handle this construct" feature is genuinely differentiated content — but the audience it attracts is overwhelmingly a *learning* audience, not a *building* audience (skeptic F18). Promoted to **content-marketing strategy** under the new Candidate 5 recommendation: teaching content is the audience-acquisition funnel that brings library authors and curious working developers to the language, not the headline pitch. See "Recommendation (revised)" below.

### Candidate 3 — "JS-first with C escape hatch for performance" (transpile-to-JS as primary, C for hotpaths)

- **User:** JS developer who occasionally needs to call into C for performance-critical work and currently has to write FFI by hand.
- **Pain:** writing both JS and C, hand-rolling the boundary.
- **Why switch:** write Waterfall once, get JS by default and C when needed.
- **Realistic addressable population:** narrow — the overlap of "comfortable with C" and "primarily writes JS" and "would switch their primary language to a small one for this." Likely small thousands.
- **Direct competitors:** WebAssembly (the obvious answer to this problem; mature; multi-language); AssemblyScript (TypeScript → WASM); Rust+wasm-bindgen; Emscripten.
- **Winning here:** would need a credible interop story Waterfall does not currently have (no FFI mechanism, no WASM target, no JS bindings beyond what the JS host provides).
- **My read:** **Wrong fit.** The pain this niche has is well-addressed by WASM. Adding Waterfall to the mix doesn't displace anything.

### Candidate 4 — "Embedded scripting for tools that need a portable runtime"

- **User:** Authors of CLI tools, config systems, or applications that need an embedded scripting language (think Lua-in-Redis, Squirrel-in-Quake, GDScript-in-Godot).
- **Pain:** Lua is dynamic; Squirrel is niche; embedding mainstream languages is heavy.
- **Why switch:** typed scripting language with a portable C runtime.
- **Realistic addressable population:** Lua's primary niche; ~3-5k active embedders.
- **Direct competitors:** Lua (deeply entrenched, 30+ year head start), Wren, Squirrel, GDScript (game engines), Janet (newer).
- **Winning here:** Would require both a stable embedding API and the C backend being genuinely link-able, which the audit shows is currently false.
- **My read:** **Wrong fit.** The current C backend can't link (C2/C3); the embedded scripting niche needs a *very* robust runtime. Multi-year detour from where we are.

### Candidate 5 — "Polyglot library author's language, with Gleam's vibe" (RECOMMENDED — REVISED)

This is the new primary niche, combining two ideas the user surfaced in conversation after Q1.

- **User:** Authors of focused, algorithm-heavy libraries who currently maintain parallel implementations across runtimes — JS/Python/C codec libraries, parser libraries, math/ML/inference kernels, crypto primitives, ZK libraries, protocol implementations, hash functions, compression routines. Often a single individual or 2–3-person team maintaining "the JS port," "the Python port," and "the C reference" of the same algorithm.
- **Pain (concrete and felt):**
  - Three implementations of the same algorithm in three languages, kept in sync by hand. Bug fixes have to be ported 3×. Test cases have to be duplicated or kept in a shared text format. Subtle drift between implementations is endemic — see the history of CRC32, SHA-256, base64, and protobuf implementations across language ecosystems for the canonical examples.
  - WebAssembly is the obvious 2026 answer, but WASM is *not* a drop-in for the library-author niche. WASM works well when you can ship a binary blob; it works poorly when the consumer wants idiomatic JS, idiomatic Python with `pip install`-able wheels, and a C header for vendoring. WASM also doesn't help the Python and C consumers feel "native" — Python users want a `setup.py`, C consumers want a `.h`.
  - Existing transpiled-to-many languages don't fit: Haxe's DNA is application code (game logic, frontend apps) — its standard library and conditional compilation are application-shaped, not library-shaped. Nim leans C with JS as an afterthought. F# Fable is overwhelmingly used for JS. None is positioned for "ship the same algorithm to npm + PyPI + a C header release."
- **Why switch (specifically to Waterfall):**
  - Write the algorithm in one Waterfall source. Get an npm package, a PyPI package, and a C single-header release from one repo, each idiomatic to its target.
  - Gleam-grade language design — sum types, `match`, `Result<T, E>`, friendly errors — without BEAM lock-in. This is the second strand of the combined positioning: developers who looked at Gleam and wished it ran on their stack get a near-equivalent aesthetic with a different target story.
  - Multi-target FFI annotations (`@external(js, ...)` / `@external(python, ...)` / `@external(c, ...)`) make the inevitable target-specific edge cases tractable.
- **Realistic addressable population:** This is the most interesting question for the new niche. The "library authors maintaining cross-runtime ports" population is smaller than "all developers" but each user has outsized leverage — one library shipped to three registries produces three points of presence. Honest estimate: low thousands of authors who would *consider* Waterfall, and capturing 50–200 of them as regular users is a Tier-B-upper-end outcome. Gleam-vibe broadens this — developers who want "Gleam without BEAM" is a much bigger pool (the "we wanted to use Gleam but couldn't" thread is one of the most common in Gleam community discussions). The combined pool is mid-thousands plausible.
- **Direct competitors:**
  - For library-author niche: WASM + bindings is the strongest competition. AssemblyScript (TS → WASM) is its closest analog — but AssemblyScript is single-target. Emscripten, Rust + wasm-bindgen are heavier and require ownership of the embedding story.
  - For Gleam-vibe niche: Gleam itself (entrenched but BEAM-locked). ReScript is OCaml-flavored. PureScript is Haskell-flavored. None occupy "Gleam's syntax aesthetic, but compiles to JS/Python/C."
  - **There is no direct entrant in the combination space.** This is the niche thesis.
- **Winning here:** Multiple library authors ship a Waterfall-built library to npm + PyPI + a C header release. The release notes credit Waterfall as the cross-runtime build system. Other library authors notice. The "I rewrote my [X] in Waterfall and now it ships everywhere" case study becomes a familiar pattern.
- **My read:** **Defensible, differentiated, and concretely useful.** The library-author niche is small but high-leverage and has no incumbent. The Gleam-vibe is the broader audience funnel that brings curious developers into the orbit. Together they form a niche that's marketable ("Gleam's vibe, your choice of runtime") and concretely solves a real problem ("ship one algorithm to many registries").

### Recommendation (revised): Candidate 5 — "Polyglot library author + Gleam-vibe combined"

**Picking Candidate 5 as Waterfall's primary niche, with Candidate 2 (teaching) reframed as content marketing.**

Rationale:
1. **It has the most concrete, defensible pain.** "Three parallel implementations of the same algorithm" is a felt pain with an identifiable user; "I want to learn how compilers work" is real but more diffuse. Concreteness compounds — case studies are tangible, before/after diffs are tangible, "this library now ships to three registries from one source" is tangible.
2. **It has no direct incumbent.** Haxe's DNA is application code. Nim, Crystal, Gleam each lock to their dominant target's runtime. WASM, the obvious competition, doesn't deliver idiomatic packages to PyPI and npm — it delivers binary blobs. There's a real space here.
3. **The Gleam-vibe layer broadens the funnel.** Pure library-author positioning is too narrow alone. "Gleam's taste, your runtime" pulls in developers who admire Gleam's design but can't or won't adopt BEAM. That funnel feeds the library-author core.
4. **Teaching content earns its keep as marketing.** A "how Waterfall compiles `match` to JS, Python, and C" post is exactly the kind of artifact a library author searches for when evaluating a multi-target compiler. The teaching content is the audience-acquisition vehicle; it's not gone, it's reframed.
5. **It's recoverable in the same way Candidate 2 was.** If the library-author pull doesn't materialize, the Gleam-vibe positioning carries us into the broader "small, well-designed language" tier as a fallback.
6. **It justifies the upper-end-of-Tier-B targets in §1.** Library-author mindshare punches above star-count (the `serde` precedent). 1500–3000 stars at v1.0 with three production library users carries more legitimacy than 1500 stars without one.

**Secondary positioning** that follows from this: "a thoughtful, modern, multi-target language" — comparable shelf with Gleam, Crystal, Nim. The flow-sensitive `readonly` is one of several design touches that signal "we're paying attention to how the language feels," not the headline.

**Explicit downsides of picking Candidate 5:**
- Library-author niches are slow-build. We won't have three production library users by v0.x; that's a v1.0 outcome, and recruiting them requires *active* relationship-building, not passive marketing.
- The combined positioning is harder to compress to one sentence than the pure teaching pitch. We commit to writing and refining the elevator pitch (see §6) until it lands.
- "Gleam's vibe" is partially aspirational — we have to *deliver* the taste, not just claim it. P11 (friendly errors) and P12 (sum types + match + Result) are not optional under this positioning; they are the deliverable that makes the pitch credible.
- WASM is a real competitive risk — see R6 (now sharpened).

**What this niche choice commits us to:**
- Each new language feature is evaluated against "does this help a library author ship the same algorithm to multiple registries?" Features that don't (e.g., effect tracking, dependent types) stay deprioritized.
- The standard library has to be *cross-target coherent*. A `Math` module that behaves identically across JS/Python/C is a marketing artifact, not just a stdlib.
- The C output has to be *consumable as a library header*, not just gcc-compilable. This pulls some C-cleanup work earlier — see §3 roadmap adjustments.
- We cultivate case-study relationships actively starting at P13. By P16 we need three of them, not zero.

---

## Section 3 — Phased Roadmap (AI-augmented, weeks-based)

This roadmap is sequenced for the **revised Tier B / Candidate-5 plan** (library-author + Gleam-vibe) built under **AI-augmented spec-first development**. Two structural changes from the prior draft:

1. **The clock is now weeks, not quarters.** Each phase is estimated in calendar weeks of side-project pace under the AI-augmented workflow (Klabnik / Carlini playbook — see `07-ai-augmented-dev-research.md`). The empirical floor (Klabnik's Rue: working compiler core in 11–14 days of evenings) supports months, not years, for technical completeness. The 5–7y calendar from the prior draft was calibrated against no-AI peer baselines (Gleam, Crystal, Nim) and overestimated by an order of magnitude for the AI-augmented build path.
2. **Module system moves earlier.** Decision: modules slot in as **P11.5** (between type inference and sum types), not P14. Reasoning below.

### Module-reordering decision

**Decision: move modules from P14 → P11.5.** Modules ship after type inference, before sum types.

The argument for keeping modules at P14 was "library authors need modules to *ship*, not to *evaluate*." Inspecting that argument carefully: it's wrong, for three reasons.

1. **The P13 stdlib is itself a multi-module artifact.** `Math`, `String`, `Array`, `IO` must be importable as separate modules to feel like a real standard library. Without a module system, stdlib is a single namespace dump — that's not "Gleam's vibe," that's MS-DOS.
2. **`wfpm new hello` in P13 should produce a multi-module-ready project structure.** A package manager that builds single-file packages is not a credible package manager. Building modules into P13's bones means we don't have to retrofit them into the package manager.
3. **Library authors evaluating the language write multi-file demos.** A candidate library author trying Waterfall during P13 will reach for modules within the first 30 minutes (they'll factor their algorithm into a `lib.wf` + `tests.wf` + `cli.wf`). If modules aren't there, the evaluation fails before it starts. The library-author niche makes the evaluation-time-not-shipping-time distinction collapse.

The cost: modules need a real type system (P11 inference) and the verifier/IR work (P10) before they can land. P11.5 is exactly the right slot — after P11's inference foundation, before P12 starts adding sum types and `@external` that themselves want to be expressed across module boundaries. Generics (with monomorphized C output) stay in P14 — they don't need to land before modules.

The added design surface from this reorder is real but contained: cross-target module emission has divergence (JS ESM exports vs Python imports vs C linking) that needs early decision. That decision becomes the gating spec for P11.5 and is the kind of design choice the AI-augmented workflow handles well *if* the spec is precise.

**Lane-discipline note (design-side consequence):** moving modules to P11.5 likely tightens what the `@external` design needs to address — visibility semantics across module boundaries, whether `@external` functions can be `pub`/`pkg`/`private`, what cross-module `@external` calls mean. This is a design-side question for the designer to think through; surfacing to the team lead, not the designer directly.

### The AI-augmented workflow per phase

Each phase below follows the same loop, based on the empirical evidence in `07-ai-augmented-dev-research.md` §3:

1. **Spec it.** Author or update `PHASE-N-design.md` and the relevant section of the language spec. The spec is the verifier — under AI-augmented development, an ambiguous spec is a bug factory (failure mode #4: silent spec resolution).
2. **Verification design (week 1 of every phase, load-bearing).** Decide: what property tests? What differential-test oracle? What adversarial inputs? Per failure mode #1 (verifier overfitting — Rue's ELF codegen bug, Carlini's hardcoded values, Bun's 13K unsafe blocks), this is the single decision that determines whether AI-built code is correct or just looks correct. Document the verification strategy in `notes/VERIFICATION-DISCIPLINE.md` (created at P10) *before* the AI starts writing implementation.
3. **Plan mode.** Open Claude Code in plan mode against the spec. Read the plan. Answer: "What ambiguities did it silently resolve?" Update spec. Re-plan. Loop until a plan is approved without spec edits. **Only then enter implementation mode.** (Per `07-ai-augmented-dev-research.md` §3 — the single highest-leverage discipline.)
4. **Implement.** Per-feature, with each commit reviewed Klabnik-style before merging.
5. **Adversarial review.** After each significant feature: open a *fresh* Skeptic session (or invoke the team's skeptic subagent) with only the spec + diff. Find bugs. Anthropic's specialized-role pattern (§3.3 of the research) suggests one dedup-pass session per phase, plus one design-critique session per phase.
6. **Phase-exit checklist (defined per phase below).** Includes the verification triad (next).

### The verification triad (replaces the mutation-test gate from round 2)

Round 2 had a "≥80% mutation-test kill rate per phase" gate. The skeptic flagged this as operationally vapor: Pitest's Kotlin plugin (`pitest-kotlin-plugin`) is unmaintained, and idiomatic Kotlin (sealed classes, `when` exhaustive, `?.let`) is structurally hard to mutation-test well. Leaning on a tool whose maintenance state is fragile is exactly the R9-shaped risk we want to avoid *in* the verification layer.

**Decision (round 3): drop the mutation-test gate. Replace with a per-phase verification triad.** All three must show "no regressions" before phase exit:

1. **Property-based tests with N=10000 generators.** Using a Kotlin property-testing framework (designer is solving the framework choice in Q11 — Kotest property arbitrers are the strongest current candidate). For each new language feature in the phase, define ≥1 invariant property (e.g., "all well-typed programs that pass the verifier emit code that runs without type errors on every backend"; "for every sum type with N variants, exhaustive `match` covers all N branches; non-exhaustive `match` is rejected"). The AI cannot tune to property tests the way it can tune to specific examples.
2. **Differential testing against a known-good oracle.** The oracle depends on the phase: (a) for P10's IR refactor, the oracle is the *existing* `*Data` AST emission (byte-equivalent goldens); (b) for new features in P11–P15, the oracle is Aaron-authored hand-checked examples; (c) for backends, the oracles are the host toolchains (gcc, Node, Python interpreters) executing the *output* and comparing against expected stdout/exit-code; (d) for phase N+1 features that rebuild on phase N behavior, phase N's behavior is the oracle. The triad fails if any oracle disagrees with the implementation.
3. **AI-generated adversarial inputs.** Pre-merge ritual per phase: ask Claude (in a fresh session, with only the spec + the feature's deliverables) to generate ≥20 "tricky inputs that should break the compiler — programs at the edge of the spec, programs that exercise interactions the test suite probably misses, programs that look correct but exploit ambiguous corners." Run each through the compiler. Document outcomes: passed, false-rejected (spec bug), or compiler-broke (implementation bug). The triad fails if any compiler-broke case ships unfixed.

This triad is **tool-agnostic and survives tool transitions** (R9 mitigation). It's also intrinsically multi-signal — passing all three is much harder to satisfice than passing a single mutation-test threshold. The cost is real (Aaron's time on differential-oracle setup, API spend on adversarial-input generation), but it's the price of the AI-augmented compression being real rather than fake.

**Where the triad lands operationally:**
- Property tests: Kotest property arbitrers, ≥1 invariant per new feature, N=10000 default.
- Differential oracle: per-phase setup, documented in `PHASE-N-design.md`.
- Adversarial inputs: per-phase pre-merge ritual, ≥20 generated inputs, results logged in `notes/VERIFICATION-DISCIPLINE.md`.

The previous round-2 mutation-test trip-wires (R8) are replaced with triad trip-wires (see R8 update below).

This is workflow overhead. The evidence (`07-ai-augmented-dev-research.md` Part 1) is that the implementation cost compresses 10–30× and the *workflow* cost is unchanged or higher than no-AI. Net compression is still large — Klabnik shipped Rue in 11–14 days following this loop — but the loop discipline is non-negotiable.

Phase numbering continues from the codebase's existing phases (the audit's last phase was 9c). I'll call my phases P10 through P17 to keep continuity, with the new P11.5 inserted between P11 and P12.

I'll annotate where AUDIT-OPEN-QUESTIONS codes (G4, G5, U1–U3, C1–C7, T1–T2) slot in.

### Pivotal phases marked **★**. Optional phases marked ○.

---

### ★ P10 — Foundation refactor: typed symbol table + IR + verifier separation
*Goal:* Make the codebase capable of supporting type inference, ADTs, and cross-cutting passes — the architectural debt items D1, D2, D3, D6 from the audit are blockers for everything downstream. **P10's spec is the single most load-bearing artifact in this entire roadmap.** If P10's design doc has ambiguities the AI silently resolves, every downstream phase inherits the wrong foundation (R10).

**Verification design (load-bearing — define before any implementation, using the triad):**
- *Property tests*: IR round-trip invariants (parse → IR → emit, original input round-trips byte-for-byte for canonical sources); type-soundness invariants (any IR node's declared type matches what the verifier assigned); ≥1 generator per IR node kind, N=10000.
- *Differential oracle*: the *current* `*Data` AST is the known-good oracle. For every existing example, the old AST and the new IR must produce identical emission (mod documented whitespace) on every backend. CI guards this strictly.
- *AI-generated adversarial inputs*: ≥20 programs designed to stress the new IR (deeply nested scopes, identifier shadowing across many levels, mixed-type expressions, programs exercising the `?` nullable parsed-but-unused syntax). Document outcomes in `notes/VERIFICATION-DISCIPLINE.md`.
- Adversarial pre-review of the P10 spec by skeptic agent before implementation begins (R10 mitigation).

**Key deliverables:**
- Replace `Any?` symbol-table info with a `SymbolInfo` struct (type, mutability, kind, source position).
- Add a public `SymbolTable.lookup` API.
- Introduce an `Ir/` package with a typed AST distinct from the ANTLR-derived `*Data` (closes D1).
- Move `verify()` into a dedicated verifier package; `*Data` classes lose the verify method (closes D3).
- Add a kind discriminator for function vs variable in the symbol table (closes D6).
- **`BUS-FACTOR.md` at repo root** (R1 mitigation, pull from year-1–3 work — costs a few hours, valuable from week 1).
- **`SPEC.md` and `CLAUDE.md` at repo root** as living artifacts (§3 workflow precondition — these are the persistent context that survives between AI sessions; spec discipline depends on them existing from P10 onward).
- **`notes/VERIFICATION-DISCIPLINE.md` at repo root** documenting the verification triad and per-phase verification logs (round-3 deliverable; F1 mitigation).
- **Gradle T2 housekeeping** — sweep Gradle 9 deprecation warnings (Q12 answer, see §7). Cheap; removes noise from every AI session log during the build sprint. Done in P10 because log signal-to-noise matters most during the AI-heavy phases.
- Audit codes consumed: depends on none; unblocks **G4** (type inference) and **U2** (C lambda lifting) by giving us a pass infrastructure. Closes **T2**.

**Success criteria:** All existing tests still pass (differential test against the old AST). A new "verify all modules before translating any" smoke test passes. Symbol table can answer "is x immutable, what's its type, where was it declared" via public APIs. Triad passes: property tests at N=10000, differential oracle byte-equivalent, adversarial inputs all classified (passed / false-rejected with spec fix / compiler-broke with implementation fix).

**Effort: 2–3 weeks calendar (AI-augmented).** This is the most architecturally consequential phase and the one where verification design *cannot* be cut. The spec/plan-mode loop will likely consume 1 of those 3 weeks; implementation is the other 1–2. **The single biggest predictor of whether the rest of the roadmap compresses is whether P10's spec is precise enough that the AI doesn't silently resolve ambiguities.** If the spec is sloppy, P10 stretches and pollutes everything downstream.

**Phase-exit checklist:**
- [ ] All three backends consume the new IR, not the `*Data` AST.
- [ ] Verifier separated; no `verify()` on `*Data`.
- [ ] `SymbolTable.lookup` public; in-test inspection demonstrated.
- [ ] CI green; all goldens unchanged (differential test).
- [ ] Property tests at N=10000 cover IR round-trip + type-soundness invariants.
- [ ] ≥20 AI-generated adversarial inputs run; outcomes logged in `notes/VERIFICATION-DISCIPLINE.md`.
- [ ] Adversarial review session ran; findings either fixed or explicitly accepted.
- [ ] `BUS-FACTOR.md`, `SPEC.md`, `CLAUDE.md`, `notes/VERIFICATION-DISCIPLINE.md` exist and are current.
- [ ] Gradle T2 deprecation warnings swept; clean build log.

---

### P11 — Type inference + condition type-checking (G4, G5)
*Goal:* Close the audit's two open type-system questions. `:=` and arithmetic and calls all flow through a real inference pass. Conditions in `if`/`while`/`for` must be `bool`.

**Verification design (triad):**
- *Property tests*: type-soundness over generated random programs (well-typed program → no runtime type error in any backend). Use a small program generator constrained to current language features. ≥1 invariant per type-rule kind, N=10000.
- *Differential oracle*: Aaron-authored hand-checked examples of inferred-type expectations (e.g., `y := add(1, 2)` infers `y: int`; `c := if true {1} else {2.0}` is a documented unify/error). For any program where the inferred type is now stricter than before, document the breaking change in `SPEC.md`.
- *AI-generated adversarial inputs*: ≥20 programs designed to stress inference — e.g., recursive functions whose return type isn't pre-declared, deeply chained calls, expressions mixing types at the edge of the unifier's reach. Negative tests for type errors: ≥20 cases covering non-bool conditions, arithmetic mismatches, function-arg mismatches, return-type mismatches.

**Key deliverables:**
- Hindley-Milner-lite inference pass over the IR.
- Reject non-bool conditions.
- Function-arg and return-type checking at call sites (closes the no-op `verify` methods flagged in the audit).
- Audit codes consumed: **G4**, **G5**.

**Success criteria:** `y := add(1, 2)` infers `y: int`. `if (1) {}` is a compile error. ≥20 new negative tests pass.

**Effort: 1 week calendar (AI-augmented).** This is mechanical implementation against a clear spec — exactly the regime where AI compresses 10–30×.

**Phase-exit checklist:**
- [ ] Inference passes for all examples.
- [ ] Negative tests cover non-bool conditions, arithmetic mismatches, function-arg mismatches, return-type mismatches.
- [ ] Property tests generating random well-typed programs at N=10000.
- [ ] ≥20 AI-generated adversarial inputs run; outcomes logged in `notes/VERIFICATION-DISCIPLINE.md`.
- [ ] No regression in existing goldens.
- [ ] Adversarial review session ran; findings either fixed or explicitly accepted.

---

### ★ P11.5 — Module system + cross-module visibility + C execution oracle (NEW — moved from P14)
*Goal:* Make `import` real. Make `Mod::fn(x)` actually link across translation units. Closes the audit's D10 (single-pass, single-module driver) and partial **C2**. **This phase moves earlier than the previous draft** because library authors evaluating Waterfall write multi-file demos within 30 minutes; the P13 stdlib is multi-module; and the P13 package manager needs the module system to be real, not retrofitted. **Round-3 addition (F4 fix): introduce C backend *execution* testing here too** — today the C runtime check is `gcc -fsyntax-only` only, which proves C *parses*, not that it *runs correctly*. The library-author niche promises "ship a vendorable C header"; if the C output compiles but segfaults at runtime, the niche promise breaks on day 1. Generics + publishing-to-registry plumbing still live in P14.

**Verification design (triad):**
- *Property tests*: cross-module visibility invariants (no public symbol from a `private` declaration leaks across modules), N=10000. Also: no two `pub` symbols in different modules collide unless explicitly imported.
- *Differential oracle (cross-module + execution-based for C)*: two-module examples must produce *behaviorally equivalent* output on all three target families. **New for C: `gcc -o exe && ./exe` execution-based testing on the C backend output, not just syntax-check.** The execution oracle is "host toolchains executing the *output* and comparing stdout/exit-code to expected values" (per §3 triad guidance, oracle type c). A "runtime-verified" subset of examples (arithmetic, control flow, function calls, array indexing, recursion, string handling) compiles AND links AND runs AND produces expected stdout. Golden stdout files captured under `compiler/src/test/resources/runtime-golden/c/`. CI runs both `gcc -fsyntax-only` (syntactic gate, existing) AND `gcc -o exe && ./exe` (execution gate, new) on the runtime-verified subset.
- *AI-generated adversarial inputs*: ≥20 cross-module programs designed to stress visibility — e.g., `pub` functions that call `private` functions from the same module, modules with circular import attempts (rejected), modules using `@external` names defined in other modules.

**Key deliverables:**
- `import Foo` and `import Foo.Bar` grammar.
- `pub` / `pkg` / `private` visibility modifiers.
- Module hierarchy (packages).
- C backend: per-module headers; multi-TU linking works. The C runtime check drops `-Wno-implicit-function-declaration` (per the audit's compiler suppressions list).
- **C backend: execution oracle ships.** Existing `gcc -fsyntax-only` syntax check stays; a new `gcc -o exe && ./exe` execution check is added in CI. Subset of examples designated as "runtime-verifiable" — the arithmetic, control-flow, function-call, recursion, array-indexing, and string-handling examples. Golden stdout files captured under `compiler/src/test/resources/runtime-golden/c/`. Expected output is the same expected output that gets captured for the JS and Python runtime checks (which already execute, not just parse). Exit code 0 required; non-zero exit is a test failure unless the example is specifically labeled "exits-nonzero."
- JS backend: pick ESM and commit; emit `export` (closes **C6**).
- Python backend: emit `import` statements correctly.
- Multi-pass driver: verify *all* modules before translating *any* (closes D10).
- **Spec section for `@external` × visibility interaction**: written but not yet implemented (implementation lands in P12 alongside `@external`). Spec only — surfaces the design-side consequence the team lead flagged.
- Audit codes consumed: **C2** (partial — full publishing pipeline waits for P14), **C6**, **D10**. Closes the audit's implicit gap where C "passing tests" today means only syntactic legality.

**Success criteria:** Two-module example links correctly in C without warning suppressions. `Mod::fn(x)` actually resolves cross-module on every backend. Negative tests for `private` symbols catch leaks at compile time. **The runtime-verified subset of examples (≥6 examples covering arithmetic, control flow, recursion, arrays, strings, multi-module) compiles, links, runs, and produces expected stdout on the C backend.** Negative tests: ≥10 cases for "private symbol accessed from another module is rejected."

**Effort: 2–3 weeks calendar (AI-augmented).** Modules are mechanical implementation once the spec is precise; the C execution oracle is integration work (CI plumbing, golden capture, choosing the subset of runtime-verifiable examples) that adds roughly half a week to the original 2-week estimate.

**Phase-exit checklist:**
- [ ] Multi-module examples compile and link across all backends.
- [ ] C runtime check drops the `-Wno-implicit-function-declaration` flag.
- [ ] Three+ examples use cross-module imports (one Aaron-authored, plus modules-of-stdlib stubs).
- [ ] **C execution oracle wired in CI**: `gcc -o exe && ./exe` runs against the runtime-verified subset; golden stdout files in place; CI fails on mismatch.
- [ ] **≥6 examples in the runtime-verifiable subset for C** (arithmetic, control flow, recursion, arrays, strings, multi-module).
- [ ] `@external` × visibility spec section drafted (implementation in P12).
- [ ] Triad passes: property tests at N=10000, differential oracle clean, ≥20 AI-generated adversarial inputs run and outcomes logged.
- [ ] Adversarial review session ran; findings either fixed or explicitly accepted.

---

### ★ P12 — Sum types + pattern matching + `Result`-style errors + `@external`
*Goal:* This is the "semantic value-add over the targets" that the landscape research said is non-negotiable for credibility. Adds the 2026 table-stakes feature set. **`readonly` (the flow-sensitive immutability feature) ships here too** under the designer's unification of declaration-form and statement-form into a single primitive.

**Verification design (triad):**
- *Property tests*: exhaustiveness check correctness — generate random sum types and `match` expressions, verify every reachable case is required (N=10000). Also: `Result<T, E>` chain composition properties (associativity of `?`-style propagation if added; identity for `Ok(x)` and `Err(e)`).
- *Differential oracle*: a `match` expression must produce behaviorally equivalent output on JS, Python, and C for all generated patterns. Use a small generator of `Result<T, E>` chains; verify all backends produce identical *runtime* output (executed via the P11.5 execution oracle for C; Node and python3 for JS/Python).
- *AI-generated adversarial inputs*: ≥20 programs designed to stress match — nested matches, `match` over deeply nested ADTs, exhaustiveness corner cases (wildcards combined with literal patterns, redundant branches, unreachable branches the verifier should reject). Plus ≥10 `@external` programs that mix targets at expression level. Plus ≥10 `readonly`-flow programs that exercise the declaration + statement forms together.

**Key deliverables:**
- Grammar: `type X = A | B | C` (sum types), `match x { A => ..., B => ... }`.
- Verifier: exhaustiveness check.
- Backends:
  - JS: tagged objects `{tag: "A", ...}` with `switch (x.tag)`.
  - Python: dataclasses + `match`/`case` (Python 3.10+).
  - C: tagged unions with `enum` + `union`.
  - Legacy: passes through.
- Replace exceptions story with `Result<T, E>` ADT in stdlib (which depends on the P11.5 module system — `Result` lives in `core::result` module).
- Lambdas get multi-statement bodies (audit Section 1: "Lambda with multi-statement body | Missing").
- **`@external(target, ...)` annotations** (Gleam-style multi-target FFI). Now interacts with the P11.5 module-visibility design — implementation lands here. A function can have separate JS / Python / C bodies; compiler tracks target support at the expression level.
- `readonly` (declaration-form + statement-form, per designer's unification). One-of-many feature per Q4; not the launch headline.
- Audit codes consumed: U1 (bundles either redefined as sum/product types or deprecated), partial U2 (multi-statement lambda bodies), R2 substrate.

**Success criteria:** `Option<T>` and `Result<T, E>` examples for all four targets. Exhaustiveness check rejects missing cases. `@external` works for JS/Python/C; mixed-support code rejected at the type level.

**Effort: 2–3 weeks calendar (AI-augmented).** Sum types and match are the most-implemented feature pattern in modern compilers — there's abundant training data, and AI compresses well here. `@external` is the novel-feature work and carries the most risk of failure mode #4 (silent spec resolution) — its spec needs to be tight.

**Phase-exit checklist:**
- [ ] Sum types and `match` work end-to-end on all three backends.
- [ ] Exhaustiveness check rejects missing cases.
- [ ] Multi-statement lambda bodies.
- [ ] `@external` works across JS/Python/C with correct visibility semantics.
- [ ] `readonly` declaration + statement forms work; verifier rejects assignment after freeze.
- [ ] Triad passes: property tests at N=10000 for match + Result, differential oracle clean across all backends (including C runtime via P11.5 oracle), ≥40 AI-generated adversarial inputs run and outcomes logged.
- [ ] Spec-level documentation of the new constructs.
- [ ] Bundles either deprecated or redefined as sum/product types.
- [ ] Adversarial review session ran; findings either fixed or explicitly accepted.
- [ ] **First niche-fit case study artifact published** (per R3 trip-wire): a before/after CRC32 (or similar) consolidation, demonstrating the parallel-implementation pain Waterfall removes.

---

### ★ P13 — Legitimacy bar: spec + LSP + package manager + cross-target-coherent stdlib + friendly errors
*Goal:* Clear the 2026 minimum viable legitimate language bar. This is the phase that converts Waterfall from "compiler experiment" to "language someone could choose." **This is the longest phase in the roadmap** — per `07-ai-augmented-dev-research.md` Part 4, LSP integration and friendly-error quality are *taste tasks* that compress only 2–5×, not 10–30×.

**Verification design:**
- LSP: integration tests against VS Code via the `vscode-test` framework. Hover-types, go-to-def, completion all tested against ≥5 example projects.
- Stdlib cross-target coherence: per-function differential test across JS/Python/C. Generated random inputs through `Math::sqrt`, `String::split`, `Array::sort` etc. must produce byte-equivalent results across all three targets. This is the *substrate* for the library-author niche; it cannot be flaky.
- Friendly errors: a panel of ≥10 hand-picked error scenarios; manual taste review by Aaron + skeptic agent. Per failure mode #6 (error messages are a taste task), AI-generated errors will be *adequate but not delightful* without explicit iteration; budget time for the iteration.
- Spec coverage: every language construct introduced through P12 has a spec section with EARS-notation acceptance criteria.

**Key deliverables:**
- **Public language spec** at v0.x. Markdown, hosted on the project site. Versioned. Covers all of P10–P12.
- **LSP server**: hover types, go-to-def, basic diagnostics, autocomplete on identifiers. Built on the new IR from P10. Kotlin-based using LSP4J.
- **VS Code extension** wrapping the LSP.
- **Package manager (`wfpm`) skeleton**: name resolution, registry stub, `wfpm install`, `wfpm new`. Registry can start as GitHub-backed (Gleam-style: any GH repo following a convention). Builds against the P11.5 module system (not retrofitted).
- **Standard library prelude with cross-target coherence**: `Math`, `String`, `Array`, `IO` modules that behave *identically* on JS, Python, and C — same arithmetic semantics, same string-encoding rules (UTF-8 everywhere), same iteration order on collections. Coherence is a published guarantee, tested per-function on every target, not a best-effort. This is the substrate that makes "ship one algorithm to three registries" credible.
- **Friendly error messages**: every error gets a source-snippet + a one-line "what's wrong" + a one-line "what to try" hint (model: Elm, Roc, Gleam). Treated as a tasted task — budget iteration time.
- **`SPEC.md` frozen at v0.9** at end of P13 (final v1.0 freeze after P16's adversarial review).
- Audit codes consumed: none (T2 already closed in P10).

**Success criteria:** LSP installable in VS Code; works on the seed Aaron-authored library. `wfpm new hello && wfpm build && wfpm publish --target js --dry-run` works. Public spec linked from README. Error-message panel of ≥10 scenarios rates ≥4/5 on friendliness (informal panel survey). Stdlib cross-target differential tests green.

**Effort: 7–13 weeks calendar (AI-augmented), median ~10 weeks (round-3 honest recalibration — was 4–6).** The longest phase. Round 2 estimated P13 at 4–6 weeks, but skeptic's component-level math is sound: six deliverables (LSP, spec, package manager skeleton, friendly errors, stdlib prelude with cross-target coherence, VS Code extension) at 2–5× compression apiece yields 7–13 weeks, not 4–6. LSP + friendly errors are the parts that don't compress fully under AI augmentation — the differential is integration with the LSP ecosystem (lots of API edge cases — vscode-test, LSP4J quirks, lifecycle messages), plus *taste* iteration on error messages (per failure mode #6 — AI errors are adequate but not delightful without explicit iteration), plus the stdlib cross-target coherence work (which is mechanical but voluminous — every function tested per-target). Package manager and spec compress fully; LSP, friendly errors, and stdlib coherence pull the median upward.

**Phase-exit checklist:**
- [ ] LSP shipping; hover types, go-to-def, completion all accurate against ≥5 example projects.
- [ ] Spec v0.9 published, dated, versioned, covers all P10–P12 constructs with EARS-notation criteria.
- [ ] `wfpm new hello && wfpm build` works end-to-end against the P11.5 module system.
- [ ] Stdlib cross-target differential tests pass per-function for `Math`, `String`, `Array`, `IO`.
- [ ] ≥10 friendly-error examples in the docs; informal panel reviewed.
- [ ] First "I tried Waterfall" external blog post or thread exists (actively solicited).
- [ ] **Second niche-fit case study artifact in cultivation** — by end of P13 we should have an external library-author candidate trying a real port.
- [ ] Adversarial review session ran; findings either fixed or explicitly accepted.
- [ ] Foundation-style brand decided (per Q7 — `waterfall-lang.dev` or equivalent); if rebrand approved, P13 is also where it happens.

---

### ★ P14 — Generics (with monomorphized C output) + publish-to-registry pipeline + idiomatic-output polish
*Goal:* Add generic type parameters with monomorphization for clean C output, build the publish-to-registry plumbing, **and absorb idiomatic-output polish into v1.0 technical** (per round-3 Mike-Test-#2). Modules already shipped in P11.5; this phase is generics + publishing + the idiomatic-output layer that library authors expect in 2026.

**Round-3 decision: idiomatic-output polish is in v1.0 technical, not deferred to v1.x.** Library-author niche is too sensitive to first-impression idiomatic output. Shipping v1.0 to npm/PyPI without source maps and `.d.ts` files is the 2026 equivalent of shipping a transpiled language in 2018 without source maps. Library authors evaluating Waterfall will judge harshly on day 1 if the package they see in their `node_modules` lacks the affordances they expect from every modern JS library. Cost: +2–3 weeks of P14 work. Acceptable.

**Verification design (triad):**
- *Property tests*: monomorphization correctness — for every generic call site, the monomorphized C function must produce behaviorally equivalent output to the JS/Python erased version (N=10000 random inputs). Source-map correctness: every source-map mapping must point to a valid source location (no orphan mappings). Type-stub correctness: every `.d.ts`-emitted type matches the Waterfall source type.
- *Differential oracle*: publishing pipeline end-to-end. From `algorithm.wf`, produce an installable npm package (with `.d.ts` and source maps), a pip-installable wheel (with type stubs and docstrings), and a C single-header release (with proper package metadata). Install each in a fresh container, exercise ≥3 functions per artifact, compare outputs across all three.
- *AI-generated adversarial inputs*: ≥20 generic-heavy programs (nested generics, generic functions returning generics, generic functions over sum types, generic containers with non-trivial element types). Plus ≥10 programs designed to stress source-map mapping accuracy.

**Key deliverables:**
- **Generic type parameters** (`fn <T> identity(x: T) returns T`) with **monomorphization for the C target** — each generic call site lowers to a concrete-typed C function. JS and Python erase generics as usual.
- C backend output as a **single-header release**: `wfpm build --target c --header-only` produces an `algorithm.h` that a downstream C project can vendor without further build steps.
- `wfpm publish --target js` produces an installable npm package.
- `wfpm publish --target python` produces a pip-installable wheel.
- `wfpm publish --target c` produces a release artifact (tarball + single-header) suitable for vendoring.
- **Idiomatic-output polish (round-3 Mike-Test-#2 absorption):**
  - **JS source maps**: every emitted `.js` file ships with an accompanying `.js.map` source map. Debuggers in Chrome / Node can step through Waterfall source.
  - **TypeScript `.d.ts` generation**: every emitted `.js` ships with a `.d.ts` containing accurate exported types derived from the Waterfall source. TypeScript consumers of the npm package get autocomplete and type-checking for free.
  - **Python docstring + type-stub emission**: every emitted Python module includes function docstrings (from Waterfall doc-comments, when present), and a `.pyi` type-stub file with accurate signatures. Pyright / mypy consumers get type-checking for free.
  - **Idiomatic package metadata**: npm `package.json` is fully populated (entry point, types field, repository, license, version per `wfpm` config); PyPI `pyproject.toml` is fully populated (classifiers, dependencies, py-typed marker); C release tarball ships with a `README.md`, `LICENSE`, semantic version tag, and a single-header file with a banner comment naming the version and source commit.
- **Seed library shipped**: Aaron-authored CRC32 (or base64 or similar — Aaron's pick) published to npm + PyPI + C header release from one Waterfall source. Published artifacts meet the idiomatic-output bar above.
- Audit codes consumed: any remaining publishing-related items.

**Success criteria:** `wfpm publish` builds installable artifacts for npm, PyPI, and a vendorable C header from a single Waterfall source — all three with idiomatic packaging (source maps, type declarations, docstrings, metadata). A generic `identity` function compiles to monomorphized C without runtime overhead. Aaron's seed library passes inspection by a library author asked "does this look like a real npm package?" — yes/no with concrete reasons.

**Effort: 4–6 weeks calendar (AI-augmented), median ~5 weeks (round-3 Mike-Test-#2 absorption added +2–3 weeks).** Monomorphization is mechanical (compresses well). The publishing pipeline has integration edges (npm registry API, PyPI upload). Idiomatic output is the new work: source-map generation needs source-position threading through the emitter (which the existing position-on-statements design supports but not on expressions — D7 from the audit is a partial blocker, may need addressing here); `.d.ts` generation and `.pyi` generation need type-information emission, which the new IR makes accessible.

**Phase-exit checklist:**
- [ ] Generic `identity` and at least one generic data structure (e.g., `Pair<A, B>`) work, with monomorphized C output verified.
- [ ] `wfpm publish` produces an installable npm package and a `pip install`-able wheel from the same source.
- [ ] A single-header C release is consumable by a downstream C project without additional build setup.
- [ ] Aaron-authored seed library (CRC32 or similar) shipped to all three registries; smoke tests pass in fresh environments.
- [ ] **JS source maps** ship with every emitted `.js`; debugger steps through Waterfall source in Chrome / Node.
- [ ] **TypeScript `.d.ts` files** ship with every emitted `.js`; TS consumer of the seed-library npm package gets accurate autocomplete and type-checking.
- [ ] **Python docstring + `.pyi` type-stub** files ship with every emitted Python module; pyright / mypy consumer of the seed-library PyPI wheel gets accurate type-checking.
- [ ] **Idiomatic package metadata** for all three targets — npm `package.json`, PyPI `pyproject.toml`, C release tarball — fully populated and inspectable by a library author as "looks like a real package."
- [ ] Triad passes: property tests at N=10000 on monomorphization, differential oracle clean across all three publishing artifacts, ≥30 AI-generated adversarial inputs run and outcomes logged.
- [ ] Adversarial review session ran; findings either fixed or explicitly accepted.

---

### P15 — Structs / records + method dispatch + generic containers
*Goal:* Fill in the C3 method dispatch story and add user-defined product types. Generics landed in P14; this phase adds records, methods, and generic containers.

**Verification design (triad):**
- *Property tests*: `Vec<T>` push/pop/get and `Map<K, V>` insert/get/delete correctness over random-sequence generators (N=10000). Invariants: round-trip (insert N items, get N items back); after-delete-not-present; iteration order matches insertion (or matches the documented ordering rule).
- *Differential oracle*: per-backend differential testing of record emission and methods. A record with mixed-type fields, constructed and accessed via methods, produces behaviorally equivalent runtime output on all three targets (executed via the P11.5 C execution oracle for C; Node and python3 for JS/Python). Generic containers compared to JS native `Array`/`Map` as the known-good oracle.
- *AI-generated adversarial inputs*: ≥20 record-heavy programs (deeply nested records, records containing arrays containing records, records with all-zero-init vs explicit-init, methods that mutate vs methods that return new records).

**Key deliverables:**
- `type Point = { x: int, y: int }` records.
- Methods on records.
- C: structs + function pointers; pick a dispatch ABI and commit. Records lower to plain C structs; methods lower to free functions taking a `Point*` receiver. Library-author friendly: no hidden vtable overhead by default.
- JS: classes or plain objects (commit).
- Python: dataclasses with methods.
- Array of records, record fields with arrays.
- Generic *containers* (`Vec<T>`, `Map<K, V>`) building on P14's monomorphization.
- Audit codes consumed: **C3** (fully), method-calls-on-type-literals (README roadmap item), array-element increment (also README), typed for-in.

**Success criteria:** A linked-list example with `match` over a `Node(value, next)` sum type works across all four targets. `Vec<T>` and `Map<K, V>` containers in stdlib.

**Effort: 1–2 weeks calendar (AI-augmented).** Records + methods are well-trodden territory; AI compresses well. Generic containers add property-based test scaffolding work; budget the extra week.

**Phase-exit checklist:**
- [ ] User-defined records with methods, across all backends.
- [ ] At least one library-author case-study example: a non-trivial algorithm (e.g., a small parser, base64, or similar) built on records + generics + sum types, publishing to npm + PyPI + a C header (with idiomatic-output polish from P14).
- [ ] Linked-list example in tests.
- [ ] Triad passes: property tests at N=10000 on `Vec<T>` and `Map<K, V>`, differential oracle clean across backends (including C runtime), ≥20 AI-generated adversarial inputs run and outcomes logged.
- [ ] Adversarial review session ran; findings either fixed or explicitly accepted.

---

### ★ P16 — v1.0 technical-complete: stabilize + release
*Goal:* Reach **v1.0 technical-complete** (per the §1 split). Feature-complete per spec, spec frozen at v1.0, all verification passes, LSP and package manager polished. **No production-user requirement at this gate** — production users are the v1.0-legitimacy milestone (12–24 month horizon, not part of the AI-augmented build sprint).

**Verification design (full triad rerun across the entire compiler):**
- *Property tests*: full project property-test suite rerun at N=10000 per invariant. No regressions from any phase.
- *Differential oracle*: cross-backend differential test across the *entire* example suite (runtime, not just goldens) — every example produces behaviorally equivalent output on JS, Python, and C, executed via the host toolchains. The P11.5 C execution oracle gates this for the runtime-verifiable subset; the rest pass syntax-only.
- *AI-generated adversarial inputs*: a fresh "find every bug" session against the entire compiler, with the skeptic given access to the full SPEC.md. Target: ≥50 inputs; document outcomes.
- *Fuzzing*: parser and verifier fuzzed with ≥1M random inputs (no crashes); ≥1K random well-typed inputs (no false-rejects).
- *Full-spec adversarial review*: spawn a fresh skeptic session with only `SPEC.md` and the implementation. Find every place where the spec admits ambiguity or the implementation diverges.

**Key deliverables:**
- Spec at v1.0; nothing breaking without a major-version bump.
- LSP feature-complete (refactor, find-references, rename).
- Package manager has ≥10 packages (mostly Aaron-authored stdlib extensions; external packages are a legitimacy-milestone deliverable, not technical-completeness).
- "Why Waterfall" blog post explaining the combined library-author + Gleam-vibe positioning.
- HN / Lobste.rs / r/programming launch post coordinated with the Aaron-authored seed library landing.
- Audit codes consumed: any remaining U1, U2, U3.

**Success criteria:** v1.0 technical-complete release notes published. The Aaron-authored seed library is installed and exercised in fresh environments for all three targets — with idiomatic-output polish meeting the "looks like a real npm/PyPI/C package" bar. Full verification triad green across the entire compiler. Fuzz suite green.

**Effort: 1–2 weeks calendar (AI-augmented).** This is a *stabilization* phase, not a feature-building phase. The case-study cultivation and community-building work (which was the bulk of the previous P16 estimate) is now properly assigned to the post-P16 adoption phase, which is calendar-bound rather than effort-bound.

**Phase-exit checklist:**
- [ ] v1.0 git tag.
- [ ] Spec frozen at v1.0.
- [ ] Full verification triad passes (property at N=10000, differential cross-backend, AI-adversarial ≥50 inputs) + fuzz suite green.
- [ ] Aaron-authored seed library installed and exercised in fresh environments for npm, PyPI, C, with idiomatic-output polish meeting the "looks like a real package" bar.
- [ ] LSP feature-complete; refactor, find-references, rename all working.
- [ ] "Why Waterfall" blog post published; launch post coordinated.
- [ ] Adversarial review session ran; findings either fixed or explicitly accepted.

---

### P16.5 — v1.0 legitimacy: production users + community (12–24 months post-P16)
*Goal:* Hit the v1.0 *legitimacy* milestone from §1. ≥3 production library users with published case studies. ≥1,500 GH stars. ≥50 active community members.

**This phase is calendar-bound, not effort-bound.** Per `07-ai-augmented-dev-research.md` §4 ("Production hardening / community trust does not compress at all"), there is no AI workflow that compresses adoption. The work is *relationship-building* with candidate library authors, content marketing, conference talks, responding to issues, and waiting. This phase is named P16.5 (rather than P17 or P18) because it is the *direct continuation* of P16 — the same v1.0 milestone, split across two clocks.

**Key deliverables:**
- **≥3 documented external production library users** — each one is a library author who shipped their library to ≥2 of {npm, PyPI, C-header release} via a single Waterfall source. Case studies published with their permission, including before/after diffs showing the parallel-implementation pain that Waterfall removed. (Cultivation starts at P13; deliverable lands here.)
- Active Discord / Zulip community with ≥50 members.
- ≥10 friendly-error iterations from real user feedback (vs. P13's panel-of-10 launch state).
- ≥5 external packages in the package manager.
- ≥1 meetup talk delivered.
- Quarterly content-marketing posts ("how Waterfall compiles X to JS, Python, and C") maintained through the year.

**Success criteria:** v1.0 legitimacy hit. ≥1,500 GH stars. ≥3 case studies published. Community channel ≥50 active members.

**Effort: 12–24 calendar months of part-time outreach + maintenance.** Roughly 5–10 hours/week of *non-coding* work, plus AI-compressed bug-fixes as users report issues. The work is materially different from P10–P16 — relationships, not commits.

**Phase-exit checklist:**
- [ ] Three external production library users documented with case studies (with their permission).
- [ ] Discord / Zulip community with ≥50 active members.
- [ ] At least one case study features a library that previously had hand-maintained parallel implementations — with the before/after diff prominent.
- [ ] ≥5 external packages in the package manager.
- [ ] ≥1 meetup talk delivered.

---

### ○ P17 — WASM target + ecosystem development (optional, post-v1)
*Goal:* Add a WASM target; develop ecosystem libraries. Pursued only if energy remains and Tier B traction is real.

**Key deliverables:** WASM backend, web playground, more stdlib, more packages, conference talks. **Skip if it competes with maintaining what exists.**

---

### Roadmap summary (AI-augmented, weeks-based)

| Phase | Goal (one sentence) | Calendar weeks | Pivotal? | Audit codes consumed |
|-------|---------------------|----------------|----------|---------------------|
| P10 | Foundation refactor — typed IR, verifier separation, spec/bus-factor/verification-discipline docs, Gradle T2 sweep | 2–3 | ★ | D1, D2, D3, D6, T2 |
| P11 | Type inference, condition checking, function-arg/return checking | 1 | | G4, G5 |
| P11.5 | **Modules + cross-module linking + visibility + C execution oracle** (round-3 F4 fix) | 2–3 | ★ | C2 (partial), C6, D10 |
| P12 | Sum types, pattern matching, Result, `@external`, `readonly` | 2–3 | ★ | U1, partial U2, R2 substrate |
| P13 | Spec + LSP + package manager + friendly errors + cross-target-coherent stdlib | **7–13 (median ~10)** | ★ | legitimacy bar |
| P14 | Generics + publish pipeline + **idiomatic-output polish** (Mike-Test-#2 absorption) | **4–6 (median ~5)** | ★ | publishing |
| P15 | Records, methods, generic containers | 1–2 | | C3, README roadmap items |
| P16 | v1.0 technical-complete: stabilize + release | 1–2 | ★ | U1, U2, U3 remainders |
| P16.5 | v1.0 legitimacy: ≥3 production library users, ≥1,500 stars | 12–24 calendar months | ★ | none — adoption phase |
| P17 | WASM, ecosystem | post-v1 | ○ | — |

**Total to v1.0 technical-complete (round-3 recalibrated): 20–31 calendar weeks (median 22–25, ≈5–6 months).** The previous round-2 estimate of "14–22 weeks, median 16–18, ≈3.5–5 months" was too optimistic because (a) P13 was under-budgeted at 4–6 weeks — the component-level math (six deliverables × 2–5× compression each) yields 7–13 weeks honestly, and (b) idiomatic-output polish was deferred to v1.x but library-author niche credibility requires it in v1.0 technical (round-3 Mike-Test-#2). New honest median lands at **~5–6 months**, still inside the original 3–6-month window but at the upper end. The total to v1.0 legitimacy is 20–31 weeks of build + 12–24 calendar months of adoption work = roughly **18–32 months from project kickoff**, still inside the §1 24-month legitimacy milestone for the median case.

**Build cadence vs adoption cadence — the conversion that actually matters under AI augmentation.** The previous draft of this section translated effort-quarters to calendar-years on an evenings-and-weekends pace. That math assumed implementation was the rate-limiter, which under AI augmentation it is not. The new conversion (from `07-ai-augmented-dev-research.md` §4):
- **Mechanical work** (parser, lexer, lowering, stdlib functions, monomorphization): AI compresses 10–30×. Klabnik's Rue: 11–14 days of evenings for a compiler core. Carlini's C compiler: 2 weeks for a Linux-buildable compiler. These set the floor.
- **Tasted work** (LSP integration, friendly errors, taste decisions in language design): AI compresses 2–5×. This is what makes P13 the longest phase.
- **Verification rigor** (property tests, differential test oracles, adversarial review): does *not* compress. In fact, requires *more* care than no-AI development. Verification design is the single load-bearing decision that determines whether the compression is real or fake.
- **Adoption / community trust** (case-study cultivation, user trust, ecosystem growth): does not compress *at all*. This is why v1.0 splits into technical-complete (compressible) and legitimacy (calendar-bound) — Bun's Rust port hit 99.8% test pass in 9 days; six months later, no one runs it in production unconditionally.

So 20–31 weeks of build + 12–24 months of adoption is the honest range. **Do not plan against the optimistic build-floor.** Plan against the median: ~5–6 months of disciplined AI-augmented work (P10–P16) followed by 18 months of relationship-driven adoption work (P16.5). If verification design slips in week 1, the build stretches toward 9 months and pollutes everything downstream.

**The four pivotal phases for v1.0 technical-complete are P10, P12, P13, P16.** P10 unblocks everything; P12 delivers the semantic value-add and the niche-fit case-study artifact; P13 clears the legitimacy bar; P16 stabilizes. P11.5 is now pivotal-but-not-marked-with-a-★ — it gates the P13 stdlib and is a niche-fit prerequisite. The other phases (P11, P14, P15) are necessary but reorderable.

**Phases that are nice but optional:**
- P17 (WASM) — defer until post-v1 unless it provides marketing leverage.
- Within P15: generic containers are optional if scope creep is biting. Records and methods are not.
- Within P12: tagged unions, `match`, and `@external` are not optional. `readonly` is one-of-many (Q4 answer). Fancy pattern features (guards, or-patterns, range patterns) are optional.

---

## Section 4 — Top Risks and Mitigations

### R1 — Burnout mid-build (rewritten — months-not-years failure modes)
Under the no-AI calibration, R1 was "5-year gap looks abandoned." Under AI augmentation, the build sprint is *months*, not years — so the failure mode shifts entirely. The new R1 is **burnout mid-build, somewhere between weeks 4 and 16**, with the spec half-written, the verifier overfit on early examples, and a growing pile of "good enough for now" decisions that the maintainer dreads revisiting. This is the version of bus-factor that actually threatens an AI-augmented sprint.

Three failure shapes:
- **Spec-writing fatigue** (most likely, weeks 1–6). Spec discipline is the most intellectually demanding part of the workflow (`07-ai-augmented-dev-research.md` §3). After 4 weeks of writing precise EARS-notation acceptance criteria for grammar features, the maintainer's tolerance for "another spec section" runs low — and the workflow degrades from spec-first to vibe-coding, at which point AI-augmented quality collapses (failure modes #1, #4).
- **Verification-triad abandonment** (weeks 6–12). After several phases where the triad (property tests + differential oracle + AI-adversarial inputs) caught real bugs, complacency sets in: "we don't really need the adversarial inputs for this small change." Then a subtle codegen bug ships, lurks for two phases, and surfaces in P14 when the publishing pipeline fails on an edge case the triad would have caught.
- **Adversarial review fatigue** (weeks 8–16). The skeptic session finds the same class of bugs repeatedly; the maintainer starts dismissing findings as nitpicks. Then a real bug hides in the dismissed pile.

**Mitigations (concrete, week-based):**
- **Phase-exit checklist as the discipline anchor.** Every phase has the same shape: spec → verification design → plan-mode → implement → adversarial review → checklist. If any item fails, the phase is not done. This makes burnout visible: when the maintainer wants to skip "adversarial review session ran," that's the trip-wire.
- **Bus-factor document (cheap, do this in P10):** a single `BUS-FACTOR.md` at repo root that lets anyone pick up the project cold. Covers: how to cut a release, how the verifier/IR pipeline is laid out, which decisions are reversible vs load-bearing. Costs ~4 hours; valuable from week 1.
- **`SPEC.md` and `CLAUDE.md` as persistent context.** Living artifacts that survive between sessions. If burnout forces a 2-week gap, these docs let the maintainer (or a future contributor) resume without re-deriving everything.
- **Permissive license** (MIT or Apache-2). Optional contributors can ever pay off only if the license allows.
- **Trip-wires (explicit revisit criteria):**
  - **Any phase-exit checklist item gets explicitly skipped:** stop and audit. This is the highest-frequency trip-wire under AI augmentation — burnout shows up as checklist-skip first.
  - **2-week build gap during P10–P16:** the sprint has stalled. Investigate cause before resuming. (Under no-AI calibration this was 9 months; under AI augmentation, 2 weeks is the appropriate signal.)
  - **Skeptic findings dismissed without a written rationale ≥3 times in a phase:** adversarial review has become theater. Pause and reset the skeptic session's framing.
  - **Any of the verification triad components is skipped in a phase** (no property tests, no differential oracle, no AI-adversarial inputs): R8 trip-wire; treat as a red flag (see R8).
  - **If P16 technical-complete slips beyond 9 months from project kickoff:** the AI-augmented compression isn't materializing. Reassess workflow rather than pushing through.

**The student-to-contributor mismatch from F18 is now less load-bearing.** Under the months-not-years build cadence, finding a second maintainer is no longer the dominant R1 mitigation — *finishing the build before burnout* is. Contributors become valuable in P16.5 (legitimacy phase, calendar-bound), which is when the calendar-bound community-cultivation work needs human bandwidth more than coding bandwidth.

### R2 — Multi-target divergence becomes structural debt
The landscape showed every multi-target language (Haxe, Nim, Gleam, F# Fable) needs an explicit target-divergence story. Waterfall currently has none — features either work on every backend or have a `TODO` placeholder.
- **Mitigation:** Adopt Gleam-style `@external(js, ...)` / `@external(c, ...)` annotations in P12 or P13. Reject mixed-support code at the type level (Gleam-style). Don't try to be the "common subset" of all four targets forever.
- **Trip-wire:** if the C backend's TODO list grows faster than it shrinks for 2 consecutive phases, escalate target-divergence design to a phase of its own.

### R3 — Niche misfit: library-author positioning doesn't attract the expected audience
The library-author niche is concrete but unproven for "language" as a vehicle (as opposed to a runtime / framework). WASM + bindings is the natural competition. Library authors are pragmatic — they will not adopt a new language just for cross-runtime publishing if the alternative is "Rust + wasm-bindgen, which I already know."
- **Mitigation:** Prove niche-fit early via concrete artifacts, not just content. By end of **P12** (when `@external` and sum types land), publish **one before/after case study**: take a small, real algorithm that's currently maintained as parallel JS/Python/C ports (CRC32, base64, or similar — Aaron's pick), rewrite it in Waterfall, and demonstrate the consolidated source vs the original three repos. This is *the* niche-fit test — if library authors don't find this artifact compelling enough to comment, share, or try, the niche thesis is wrong.
- **Secondary mitigation (audience funnel):** the Gleam-vibe layer + content-marketing (former Candidate 2) artifacts ramp in parallel. The first two teaching artifacts (`match` lowering across backends, control-flow translation comparison) ship by end of P11 as the *funnel*, not the validation. They draw the broader curious-developer audience; the case study converts the subset who are library authors.
- **Trip-wire (case-study channel):** if the first before/after case study fails to attract any inbound interest from library authors within 90 days of publishing, **reconsider niche before P14** — specifically, before committing the publishing-infrastructure work that P14 demands.
- **Trip-wire (funnel channel):** if the first two teaching artifacts get fewer than 200 reads/views each within their first month, the broader audience funnel is underperforming and content strategy needs reassessment.

### R4 — Feature creep / Tier C drift
The temptation to add features that look "real" — concurrency, FFI to specific targets, IDE features beyond LSP basics — will pull resources from the legitimacy-bar phases.
- **Mitigation:** Stick to the phase plan. Treat off-roadmap features as "fun side branches that don't merge to master." Maintain a "deferred" list (P17 is partly that) and revisit only after v1.0.
- **Trip-wire:** if a single phase exceeds 1.5x its effort estimate, audit for scope creep.

### R5 — The "novel readonly" feature doesn't pan out and we've committed to it publicly
The landscape research noted Waterfall's proposed `readonly` is largely novel — but novel can mean "good idea nobody had" or "bad idea everyone considered and rejected." Pony's `recover` and Rust shadowing exist as workarounds for adjacent needs.
- **Mitigation:** Don't market `readonly` as Waterfall's headline feature until after P12 implementation experience confirms it. Implement it; use it internally for 1-2 examples; if it feels good, promote it. If it feels awkward, downgrade to "one of several modifiers" and don't make it the pitch.
- **Trip-wire:** if `readonly` examples in tests don't read well to fresh eyes by end of P12, deprioritize the marketing.

### R6 — WASM eats the library-author niche (heightened under the new positioning)
The library-author niche is *especially* sensitive to WASM. The 2026 trajectory is unambiguous: WASM-based cross-runtime publishing is improving on every axis (component model, WASI preview 3, native ESM integration in Node, `wasm-bindgen` ergonomics). If WASM-via-Rust becomes the dominant way to ship a single-source library to multiple runtimes, the library-author niche evaporates. CoffeeScript's death-by-ES6 is the cardinal cautionary tale; the WASM-eats-cross-runtime scenario is the modern equivalent.
- **Mitigation:** Anchor Waterfall's value-prop in places WASM is *structurally* worse: (1) **idiomatic outputs** — npm packages with native JS, PyPI wheels that look like real Python, C headers that vendor cleanly, not binary blobs and bindings; (2) **readability** — humans read Waterfall's emitted JS/Python/C; nobody reads WASM bytecode; (3) **Gleam-grade language design** that WASM-via-Rust doesn't offer (Rust is great but heavy; library authors don't always want to commit to Rust's ergonomic ceiling for a small algorithm). The pitch is "you can read what Waterfall produced, and what it produced is idiomatic in each target — WASM gives you neither."
- **Active monitoring:** track the WASM ecosystem quarterly. Specifically watch for (a) `wasm-bindgen` adding a "produce idiomatic Python wheel" mode, (b) a major library (e.g., a crypto primitive, a hash function) switching from parallel implementations to WASM-via-Rust as the canonical distribution, (c) Mojo or Carbon shipping a credible cross-runtime story.
- **Trip-wire:** if a credible "single-source-to-idiomatic-npm-and-PyPI" WASM tool ships and gets adoption within 12 months, **reassess the niche thesis from first principles** — don't try to out-compete WASM head-on; pivot toward the Gleam-vibe (broader, less WASM-threatened) side of the combined positioning, and let the library-author claim soften.

### R7 — Long-tail / post-build burnout (P16.5 phase)
Distinct from R1 (mid-build burnout, weeks 4–16). R7 is the *post-technical-complete* version: P16 ships, the public launch lands, and then the 12–24-month adoption phase is a different kind of work — slower, less obvious progress, more outreach-flavored. The maintainer ships the v1.0 launch post, gets some HN traction, and then experiences a motivational dip when v1.0 legitimacy doesn't materialize in the expected 90 days.
- **Mitigation:** Set realistic 6-month checkpoints during P16.5. The legitimacy milestone is calendar-bound, not build-bound — expecting it on a build clock guarantees disappointment. Keep ~5–10 hours/week of *coding* work (bug-fix releases, small features, content marketing) as the engagement vehicle; pure outreach is harder to sustain alone.
- **Trip-wire:** if you go 60 days without *any* project activity (commit, post, issue response) during P16.5, ask honestly whether the project is still alive. If yes, restart with a small visible release. If no, archive it gracefully.

### R8 — Verification overfitting (the dominant AI failure pattern)
**This is the single biggest risk added under AI augmentation, and it's a HIGH severity bug pattern by every credibility metric in the research** (`07-ai-augmented-dev-research.md` §2, failure mode #1). AI agents *satisfice the verification signal*. They will pass tests by hardcoding values, by handling only the inputs the tests cover, or by introducing wrong constants that happen to produce correct outputs for tested cases. Evidence base: Klabnik's Rue ELF codegen segfaulted because instruction-size constants were hardcoded incorrectly but didn't matter for the trivial test programs; Anthropic's C compiler "optimizes for passing tests rather than correctness, hard-codes values to satisfy the test suite, and won't generalize" (Leo de Moura's analysis); Bun's Rust port hit 99.8% test pass with **13,000 unsafe blocks** (180× higher density than uv) that the test suite does not exercise.

- **Mitigation: the verification triad (round-3 replacement for the round-2 mutation-test gate).** Every phase runs all three legs; all three must show "no regressions" before phase exit:
  1. **Property tests at N=10000** (Kotest property arbitrers — pending designer's Q11 framework choice). For each new feature, ≥1 invariant property. The AI cannot satisfice random-input property tests the way it can specific examples.
  2. **Differential oracle.** Per-phase oracle setup documented in `PHASE-N-design.md`. Oracle types: (a) old AST for P10; (b) Aaron-authored hand-checked examples for new features; (c) host toolchains executing output and comparing stdout/exit-code for backends — *this is where the P11.5 C execution oracle is the model*; (d) phase N-1's behavior as oracle for phase N where features compose.
  3. **AI-generated adversarial inputs (≥20 per phase).** Pre-merge ritual: fresh Claude session with only spec + deliverables, asked to generate "tricky inputs that should break the compiler." Run all through the compiler; document passed / spec-bug-fixed / impl-bug-fixed.
- **Why the triad, not mutation testing.** Round 2 specified ≥80% mutation-test kill rate per phase. Skeptic flagged this as operationally vapor: Pitest's Kotlin plugin (`pitest-kotlin-plugin`) is unmaintained, and idiomatic Kotlin (sealed classes, exhaustive `when`, `?.let`) is structurally hard to mutation-test well. Leaning on a fragile tool inside the verification layer is exactly R9-shaped. The triad is tool-agnostic, multi-signal, and survives tool transitions.
- **Supporting practices** (also non-negotiable):
  - **Prompt-context independence.** Verification artifacts (property tests, oracle examples, adversarial inputs) come from a different session than the implementation, with no shared chat history. Same model is fine; same conversation is not. The implementation reasons from "what I've already coded"; the verification reasons from the SPEC. Without this independence, both sessions share silent assumptions and the verification overfit becomes invisible. Aaron's role on verifier-correctness paths is reviewer, not author — push back on PRs whose verification artifacts read wrong.
  - **Fuzzing.** Parser and verifier get ≥1M random inputs at P16; no crashes, no false-rejects on well-typed programs.
- **Trip-wire**: if **any verification triad leg is skipped in any P10–P15 phase** (no property tests, no differential oracle, fewer than 20 AI-adversarial inputs), treat that as a red flag. Do not advance to the next phase until the gap is closed. The cost of letting an under-verified phase ship is that every downstream phase inherits the under-verified foundation. Sub-trip-wire: if a phase consumes any of the triad outputs *and* the next phase's first ≥3 bugs all trace back to that phase, the triad was inadequate — pause and revise the triad design before advancing further.

### R9 — AI tooling regression / dependency on tool quality
Aaron's workflow depends on Claude Code / Codex / Cursor quality remaining steady or improving over the build sprint. If a major Anthropic / OpenAI model release degrades on compiler work specifically, or if a tool's plan-mode / spec-discipline support regresses, the build cadence drops dramatically. This is not a hypothetical: Klabnik's "tale of two Claudes" post documents Claude Opus 4.5 vs 4.6 differences material to compiler work; the Bun port's quality is partly tool-version-bound.
- **Mitigation:**
  - **Specs are independent of tool quality.** `SPEC.md`, `PHASE-N-design.md`, and the language reference spec are *the durable artifacts*. If AI quality degrades, the specs are still implementable by humans — slower, but possible. This is the actual fallback: not a different AI, but a different workflow (humans implementing the spec).
  - **No tool-vendor lock-in in the workflow.** The spec-first / plan-mode / adversarial-review pattern works across Claude Code, Codex, Cursor, and even fully human implementation. The dependency is on *the workflow*, not on any one tool. Document the workflow in `CLAUDE.md` (and equivalent) so future tool transitions don't lose institutional knowledge.
  - **Cross-vendor verification.** When adversarial review fires, use a *different* model for the skeptic than for the builder where possible (Claude builds, Codex reviews; or vice versa). This is the [alecnielsen/adversarial-review](https://github.com/alecnielsen/adversarial-review) pattern. Cross-vendor catches model-specific bugs that intra-vendor review misses.
- **Trip-wire:** if Aaron has to **abandon AI tooling for a phase entirely** (e.g., model quality regresses on compiler work and produces unreliable output), the phase budget doubles, and the v1.0 technical-complete milestone slips toward the 9-month worst case from §1. Reassess at that point.

### R10 — Verifier-design quality in week 1 (specifically P10)
P10's spec is the single most load-bearing artifact in the roadmap. If the P10 design doc has ambiguities — what does the IR look like? what's the SymbolInfo shape? how do verifier passes compose? — the AI will silently resolve them, and every downstream phase inherits the wrong foundation. This is failure mode #4 (`07-ai-augmented-dev-research.md`) in the highest-stakes location.
- **Mitigation (concrete):**
  - **Adversarial pre-review of the P10 spec.** Before any P10 implementation begins, spawn a fresh skeptic session with only the P10 design doc. Ask: "What ambiguities exist here? What decisions are unstated?" Update the spec to remove all surfaced ambiguities. Re-review. Loop until the skeptic finds nothing material.
  - **Plan mode discipline at P10 in particular.** P10 plan-mode iterations should average ≥3 before approval. If the first plan is approved without spec edits, the spec is probably too thin.
  - **EARS-notation acceptance criteria for every P10 deliverable.** "WHEN [condition], THE SYSTEM SHALL [behavior]." Forces specs to be testable.
  - **Treat P10 as the spec template for the entire project.** If P10's spec discipline is solid, every later phase inherits the same shape. If P10's spec is sloppy, every later phase is also sloppy by default.
- **Trip-wire:** if P10 implementation reveals an unstated design assumption — i.e., a question of the form "wait, how was this supposed to work?" — pause implementation, update the spec, re-plan. Do not try to "negotiate" the implementation around an under-specified design.

---

## Section 5 — Explicit Non-Goals

These are things Waterfall is **not** going to be. Each non-goal is here because someone (the user, a future contributor, or a curious onlooker) will be tempted to suggest it.

### NG1 — Not a JVM-compatible language
We will not target the JVM (despite the compiler being on the JVM). The four current targets are enough. Kotlin/JS, Scala.js, F# Fable already occupy the "JVM language with multi-target" niche; we don't compete there.

### NG2 — Not aiming for vendor adoption
We will not pitch Microsoft, Google, Apple, or JetBrains to back the language. Tier C is not the goal. If a vendor approaches us, fine — but no time spent chasing them.

### NG3 — Not a research vehicle for novel type theory
Waterfall is not the place to prove out dependent types, gradual typing, effect systems, or session types. Hindley-Milner-lite is the ceiling for the type system through P15. Research-grade features come from research languages (Idris, Koka, Granule); we cite them, we don't compete with them.

### NG4 — Not a "fast" language
We don't compete on runtime performance. The C backend exists, but performance is incidental — the JS and Python backends are equally legitimate citizens. Anyone benchmarking us against Zig or Rust is reading the language wrong, and we don't engage that framing.

### NG5 — Not a systems / OS language
No kernel writing. No driver writing. No "C replacement" pitch. The C backend exists for portability and teaching, not for replacing C in serious deployments.

### NG6 — Not a concurrency-focused language
No goroutines, no async/await, no STM, no actors in v1. Add this only if a real user pushes for it post-v1, and only with a clear cross-target story.

### NG7 — Not a domain-specific language
We won't pitch Waterfall as "for game dev" or "for data science" or "for embedded." Generality is the point. Domain framing is what trapped Haxe in the games niche; we avoid the trap.

### NG8 — Not a language for everyone
We're not chasing "approachable for total beginners" or "loved by C++ veterans." Our user is the curious working developer / CS student. Other audiences are not negative-space — they're just not the target.

### NG9 — Not maintained at a corporate cadence
We're not promising monthly releases, SLAs on bug reports, or 24-hour response times. Indie cadence is honest cadence.

### NG10 — Not going to fork or compete with another small language
If someone says "you should merge with Gleam" or "fork Nim," the answer is no. Each small language has its own identity; ours is forming. Merging dilutes.

---

## Section 6 — Marketing / Community Story

The landscape research's legitimacy bar requires:
1. LSP / editor support → **P13 deliverable.**
2. Public spec → **P13 deliverable.**
3. Package manager → **P13 deliverable.**
4. At least one production user → **P16 deliverable** — revised to **three production library users** under the new niche.
5. Friendly error messages → **P13 deliverable.**
6. A clear semantic value-add → **P12 deliverable** (sum types + match + Result).
7. A target-divergence story → **P12 deliverable** (`@external` annotations — promoted from "P12 or P13" to firmly P12 under the new niche).
8. **Cross-target coherent stdlib** (added under the library-author positioning) → **P13 deliverable.**
9. **Publish-to-registry pipeline** (`wfpm publish` producing idiomatic npm packages, PyPI wheels, and C headers from one source) → **P14 deliverable.**

Waterfall's equivalents, specified:

### Spec
A markdown spec at `spec/` in the repo. Versioned. Lives at `waterfall.dev/spec` (or wherever we land — domain selection itself is one of the Section 7 open questions). Each new language feature requires a spec PR before implementation. Patterns to copy: Gleam's spec is on their website; Crystal's is on `crystal-lang.org`. Reject the "no public spec" approach (which is the audit-revealed current state).

### Tooling (LSP)
A Kotlin-based LSP server using LSP4J (the Java-ecosystem standard library for LSP), bundled with a VS Code extension. JetBrains plugin secondary, post-v1.

### Package manager (`wfpm`)
Gleam-style: any GitHub repo following a convention. `wfpm.toml` or `Wfpm.kt`-style config (commit one). `wfpm install` resolves names from a registry (start with a GitHub-hosted index file).

### Production library users (Section 1's 5y milestone — revised)
We won't find production library users by accident, and "any production user" is the wrong target. We need **three documented production library users by v1.0**, each one a library author who shipped *their* library to multiple package registries via Waterfall. Cultivation starts at P13 — by the time P13 publishes the LSP and package manager, we should already have 1–2 candidate library authors trying Waterfall on small ports. By P14, when `wfpm publish` lands, we have a working version of the pipeline that produces an npm package + a PyPI wheel + a vendorable C header from one source — and we use it to ship at least one Aaron-authored library (a CRC32, a base64, a small parser — whichever is most useful and demonstrable). That seed library is the proof-of-concept that recruits the next two case studies.

### Friendly errors
Every error message has a (1) source-snippet showing the line, (2) one-line "what's wrong," (3) one-line "what to try." This costs one Friday afternoon per error class. Cumulative effort is high; per-error it's cheap. **Bake it in from P11 onward**; don't retrofit at the end. Under the Gleam-vibe positioning this is non-negotiable — error messages are part of the "taste" pitch.

### Target-divergence
P12 introduces `@external(target, ...)` annotations Gleam-style. Specifies which target a function works on. The compiler tracks at the expression level. This is one of the few specific design ideas worth borrowing wholesale. Under the library-author positioning, this is the central interop primitive — promoted from a P13-or-later afterthought to a P12 deliverable (see §3).

### Positioning statement (one line)
**Waterfall is a small, typed language for shipping one algorithm to many runtimes — write it once in Waterfall, publish it to npm, PyPI, and as a C header.**

### Elevator pitch (three sentences)
Waterfall is a small, typed language for code that needs to ship across runtimes. Write your algorithm once in Waterfall — with sum types, `match`, friendly errors, and Gleam-grade language design — and publish the same source as an npm package, a PyPI wheel, and a vendorable C header. If you've ever maintained parallel JS, Python, and C ports of the same library, Waterfall is the tool that stops the drift.

### Content marketing strategy (former Candidate 2, now the audience-acquisition funnel)
Teaching content is reframed as **content marketing for the library-author audience**. The "how Waterfall compiles X to each target" series is now an audience-acquisition vehicle: library authors evaluating a multi-target compiler want to *see* the JS, Python, and C output side by side before they commit. The same content that would attract a CS student now attracts a library author who is asking the same question — "what will this look like in my consumers' hands?" — for different reasons.

Concrete content cadence:
- **Monthly: a "Waterfall compiles X" deep-dive.** X cycles through control flow, pattern matching, generics, FFI, error handling, stdlib functions. Each post shows the Waterfall source, the JS output, the Python output, the C output, side by side, with annotations. These are the artifacts that show up in library-author search results.
- **Per-feature-release: a feature-launch post.** Shorter; explains why a new feature exists and what shipping it enables in library code.
- **Per-quarter: a "case study" post.** Initially Aaron-authored; by P15 onward includes external library authors.
- **Conference talk per year** (meetup-tier or higher): same content, different format.

### First three case studies (concrete proposals)
The case studies are the load-bearing P16 deliverable. To make them credible, they should be specifically the *shape* of pain the niche solves. Proposed:

1. **"I rewrote CRC32 in Waterfall and now it ships to npm, PyPI, and as a C header."** Aaron-authored (P14 deliverable, becomes the seed case study). Show the original three repos (or representative open-source equivalents), the consolidated Waterfall source, and the published artifacts. Show that the test vectors pass identically on all three runtimes from one source of truth.
2. **A small parser** — perhaps a TOML subset, a CSV parser with edge cases, a simple expression evaluator — by an external library author who currently maintains parallel implementations. Cultivated starting at P13.
3. **A crypto or hashing primitive** — perhaps a Blake3 variant or a small signature scheme — by an external author. The crypto/ZK adjacency is high-value because that audience is *especially* burdened by parallel-implementation drift (and is willing to invest in tooling that removes it).

Each case study includes: the before/after diff (lines of code maintained), the test surface (vectors and how they're shared), the failure modes the parallel-implementation approach had, what Waterfall changed in their workflow.

### Community channels
- **Discord** (modern default for small languages). Library-author niche needs a real-time channel where authors can ask "how do I bind this to my npm consumer?"-style questions.
- **GitHub Discussions** for slower / archivable conversations.
- **Mastodon / Bluesky** for outreach (no Twitter strategy).
- **HN / Lobste.rs / r/ProgrammingLanguages** for release-time pushes.
- **r/rust, r/javascript, r/Python** for content-marketing post seeding (where library authors actually hang out).

### The "first 100 users" plan (recalibrated to AI-augmented timeline)
Library-author niches build through ones and twos, not virality. Target sequence (recalibrated against the months-to-v1.0-technical timeline; calendar-bound milestones for legitimacy):
1. By **end of P12 (~weeks 5–8)**: ~5 curious developers who saw the seed case-study artifact (CRC32 before/after) and tried `wfpm new hello`. Discord seeded.
2. By **end of P13 (~weeks 9–14)**: ~15 active developers; 1–2 library-author candidates trying real ports as evaluation. First "I tried Waterfall" external blog post or thread exists.
3. By **end of P14 (~weeks 12–17)**: ~30 active users, ≥3 library-author candidates. Aaron-authored seed library published to all three registries.
4. By **end of P16 / v1.0 technical-complete (~weeks 14–22)**: ~50 active users; v1.0 launch post lands on HN/Lobste.rs/r/programming. First external library-author still cultivating their case study (not yet published).
5. By **+12 months post-P16 (P16.5)**: 75+ active users, first external case study published, second in cultivation. ≥500 GH stars.
6. By **+18–24 months post-P16 (v1.0 legitimacy)**: 100+ active users, three published case studies, ≥1,500 stars.

Acquisition is *direct outreach* throughout P12–P16 and the first 6 months of P16.5 (Aaron personally inviting candidate library authors), shifting to inbound (case studies + content marketing) by month 12 of P16.5.

### The AI-augmented build as part of the narrative
Under AI augmentation, Waterfall's *build story* itself becomes a marketing artifact. The empirical landscape (`07-ai-augmented-dev-research.md`) makes "indie maintainer ships a typed multi-target compiler in months, not years" a credible and current thing — Klabnik's Rue is the public template; Lambeau's Elo is the existence proof for tiny scope; Carlini's C compiler is the upper bound.

**Recommendation on how to handle the AI-augmented build story (the call):** **be transparent but don't lead with it.** The pitch remains "ship one algorithm to many runtimes" — not "AI built this." Two reasons:
1. **Library authors are skeptical of AI-built compilers, for reasons grounded in evidence** (Bun's 13K unsafe blocks, Hejlsberg's TS→Go skepticism). Leading with AI risks triggering exactly the skepticism the niche has the least tolerance for — library authors care about correctness more than most consumers.
2. **The Gleam-vibe positioning rewards taste, not novelty of construction.** "AI-built" is a construction story; "tasted language design with friendly errors" is a product story. Lead with the product.

But **transparency matters when asked**. The repo's `README.md` and `CLAUDE.md` should clearly state: "Waterfall is developed with AI augmentation (Claude Code in plan-mode-first workflow) under adversarial review; the spec, design decisions, error-message polish, and verification strategy are human-authored; mechanical implementation is AI-assisted." This is also evidence for **R9 mitigation** — the spec-first artifacts are durable across tool transitions.

Content marketing can reference the build approach explicitly in occasional posts ("How I built a multi-target compiler in 4 months with AI augmentation: what worked, what didn't"), positioned as *engineering narrative*, not as the product pitch. This is the highest-leverage way to use the AI-augmented-build story without making it load-bearing for adoption.

---

## Section 7 — Open Strategic Questions for Aaron

These are the questions I can't decide for you. They're listed roughly in order of urgency (highest first).

### Q1 — Do you accept the Tier B / teaching-niche thesis? **(ANSWERED)**
**Answer received:** Tier B confirmed, **upper end**. Niche changed from teaching to **Candidate 5 (polyglot library author + Gleam-vibe combined)**; teaching content is reframed as the content-marketing funnel, not the headline pitch. The rest of this document has been revised accordingly: §1 success criteria upgraded to upper-end-of-Tier-B (1,500 stars at v1.0, three production library users); §2 has a new Candidate 5 recommendation; §3 pulls `@external` to P12, stdlib coherence to P13, and generics-monomorphized-for-C to P14; §4 R3 and R6 reframed for the new niche; §6 elevator pitch and case-study plan rewritten.

### Q2 — Is the timeline acceptable? **(ANSWERED — split into v1.0 technical and v1.0 legitimacy)**
**Answer received:** AI-augmented build accepted. v1.0 **technical-complete** target: 3–6 months (median 5–6) per `07-ai-augmented-dev-research.md`. v1.0 **legitimacy** target: 12–24 months from project kickoff, including the 12–24-month adoption phase (P16.5) that AI cannot compress. §1 milestones rewritten; §3 phases recalibrated to calendar weeks (14–22 weeks total to technical-complete). "Some bugs OK until adoption" accepted as the technical-complete bar.

### Q8 — How transparent about the AI-augmented build? (NEW)
The build process is a marketing artifact under AI augmentation — but library authors are skeptical of AI-built compilers for evidence-based reasons (Bun's unsafe-block density, Hejlsberg's skepticism). Recommended posture (§6): **transparent but not leading**. README and CLAUDE.md disclose the workflow; the pitch stays "ship one algorithm to many runtimes." Occasional engineering-narrative posts about the build process are fine; the pitch is not. **Lead-with-AI / transparent-but-not-leading (recommended) / silent-on-AI.**

### Q9 — Adversarial review budget — explicit dollars/hours per phase (NEW)
The §3 workflow assumes per-phase adversarial-review sessions (fresh skeptic against spec + diff). Concretely, this means either (a) running a multi-agent team (Skeptic, Critic, Dedup-pass) at the end of each phase via Claude Code subagents — API spend ~$50–200/phase for a thorough session, or (b) using `/ultrareview` or similar at major phase boundaries — higher quality, ~$100–500/phase. Under AI-augmented build, the verification rigor is *exactly* what determines whether compression is real or fake (R8, R10). Skimping here defeats the entire timeline. **OK with $50–500/phase budget / lower budget / different review approach.**

### Q3 — Are you willing to take on a second maintainer by P14?
**Weight shifted under the new niche.** R1 (bus factor) is still the highest project risk, but the library-author niche delivers a *slightly* better contributor pipeline than the teaching niche did — library authors using Waterfall in production have a direct incentive to upstream fixes for bugs that block their releases (their use case is broken until the fix lands), whereas teaching-content readers have no equivalent forcing function. So the question shifts from "will a contributor materialize?" (no, under the old niche) to "when do we *actively recruit* one?". Recommend deferring to **P14** (later than the original Q3's P12) because P10–P12 are foundation work where one-maintainer focus is faster than two-maintainer coordination. By P14, the publish-to-registry plumbing creates real surface area where a second contributor adds leverage. **Yes / no / "depends on who" / "later than P14."**

### Q4 — Do you want `readonly` to be the headline differentiator, or just one of several features?
**Weight decreased under the new niche.** Under the library-author + Gleam-vibe positioning, the headline is the niche fit ("ship one algorithm everywhere") and the language design taste, not any single novel feature. `readonly` is one of several language-design touches that signal "we paid attention," not the lead. Recommend treating it as **one-of-many** under the new positioning — implement it cleanly in P12, mention it in the design notes, but don't make it the launch story. **Headline / one-of-many (recommended) / kill the feature.**

### Q5 — Are you willing to drop the "legacy" backend?
The audit shows the legacy backend is C++-flavored, holds quirks the other three don't (backticks in strings, `for (auto x : c)`), and exists mainly as a regression anchor. Maintaining four backends is expensive; three would be cheaper and cleaner. **Drop legacy / keep legacy as regression anchor / keep legacy as a first-class target.**

### Q6 — Are you willing to add a WASM target eventually?
P17 is currently optional. If WASM is a "yes, eventually," that affects spec design decisions in P13 (e.g., do we make the integer model strict so WASM emission is straightforward?). If WASM is a "no," we can be looser with target-specific quirks. **Yes-eventually / no / undecided.**

### Q7 — Are you willing to publish under a foundation-style name eventually? **(weight increased)**
**More important under the new niche.** Library authors evaluating a multi-target compiler treat brand stability as a proxy for project longevity — "will this still be here in 3 years when my npm package's users complain?" A foundation-style brand (`waterfall-lang.dev` rather than `github.com/AaronCoplan/waterfall`) signals "this is bigger than one person" in exactly the way that helps a candidate library author commit to migrating their algorithm. Recommend rebrand at P13 (alongside the spec publication), not P13–P14 as before — earlier is better for the niche. **Yes / no / "let's see."**

### Q12 — When to address the Gradle 9 deprecation warnings (T2)? **(ANSWERED — recommend P10)**
Audit's T2 is noted as "not urgent." Round-3 proposal: address in **P10 housekeeping** alongside the other P10 docs (`BUS-FACTOR.md`, `SPEC.md`, `CLAUDE.md`, `notes/VERIFICATION-DISCIPLINE.md`). Reasoning: cheap (few hours), and removes warning noise from every AI session log during the build sprint. The cost of "Gradle output drowns the actual signal" during the AI-heavy phases is real — Aaron is going to be reading log output constantly, and a clean build log dramatically improves the signal-to-noise of triad outputs and adversarial-review findings. Alternative (b) "P13 polish, when other QoL items also land" is fine too but loses 4–6 months of cleaner logs first. Alternative (c) "never, let it ride" is rejected — Gradle 9 will eventually drop support, and the cost-to-fix doesn't go down. Already integrated into P10 deliverables above. **Confirm P10 / different phase / never.**

---

## Closing thought (not a section)

The most opinionated single call in this document (revised) is that **Section 1's upper-end-of-Tier-B recommendation and Section 2's Candidate-5 niche (library-authors + Gleam-vibe) are coupled and load-bearing on each other**. The upper-end star and case-study targets in §1 are justified *because* library-author mindshare punches above its star-count; the library-author niche is *plausible at scale* only because Tier B's legitimacy investments (LSP, spec, package manager, friendly errors) clear the bar that library-author evaluators apply. Picking one without the other doesn't work.

The second-most opinionated call (unchanged) is that **P10 (foundation refactor) must come first, despite being invisible work**. Under AI-augmented build cadence this becomes *more* important, not less — P10's spec is the foundation that determines whether every downstream phase compresses or stretches (R10). The audit identified D1–D10 as architectural debt items, and adding features on top of D1 (no IR), D2 (`Any?` symbol table), and D3 (entwined verify/translate) means re-doing the work twice — and the second time, with an AI that confidently rebuilds the same wrong abstractions.

The third call, surfaced by the niche pivot: **teaching content is the audience-acquisition funnel, not the destination**. The deep-dive posts earn their keep by bringing curious developers — some fraction of whom are library authors — into the orbit.

The fourth call (new under AI augmentation): **v1.0 splits into "technical-complete" (months, AI-compressible) and "legitimacy" (months-to-years, not compressible)**. The build sprint is materially different from the adoption phase. Planning them as one milestone — as the previous draft did — guarantees mis-calibration on at least one side. Plan them separately. Build relentlessly during the sprint; cultivate relentlessly during the adoption phase.

The fifth call (the load-bearing one under AI augmentation): **verification discipline in week 1 of P10 is the single most consequential decision in this entire roadmap.** Everything else compresses; this doesn't. If verification discipline slips, the AI-augmented compression is fake — the code looks done and isn't — and the rest of the strategy doesn't matter because the foundation is wrong. The verification triad — property tests at N=10000, differential oracle per phase (including the round-3 C execution oracle from P11.5 onward), ≥20 AI-generated adversarial inputs per phase — plus fresh-session adversarial review per phase: these are non-negotiable, and the costs (API budget, time spent on tests rather than features) are the price of the compression being real. The triad replaces the round-2 mutation-test slogan with operationalizable, tool-agnostic discipline.

The sixth call (new in round 3): **idiomatic-output polish belongs in v1.0 technical, not deferred to v1.x.** Library-author niche credibility is decided on day 1 by what the published npm package, PyPI wheel, and C release look like. Source maps, `.d.ts`, docstrings, package metadata — these are the table-stakes that distinguish "a real package" from "a transpiler experiment that happens to be installable." Costs +2–3 weeks in P14; preserves the niche thesis.

Everything else is recoverable.
