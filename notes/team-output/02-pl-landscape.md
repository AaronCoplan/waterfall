# 02 — Programming Language Landscape (External Research)

Author: language-researcher (Task #2)
Date: 2026-05-14
Sources cited inline via markdown links.

---

## Executive Summary

- **Transpiled languages overwhelmingly succeed or fail on a single axis: does the target language already do what you want, well enough, with native momentum?** TypeScript's win story is essentially "JavaScript desperately needed types, and adding them gradually was acceptable." CoffeeScript's loss story is essentially "JavaScript got the features CoffeeScript added (ES6), so the value proposition evaporated."
- **The "minimum viable legitimate language" bar in 2026 is much higher than it was when CoffeeScript launched (2009).** Today's bar includes: a stable type system that integrates with editor tooling, a package manager and standard library, a documented spec, an interop story with the target, and at least one well-known production user. Below that bar, the language reads as a hobby experiment regardless of technical merit.
- **Multi-target transpilation has two architectural patterns: conditional code (Haxe, Gleam, Nim) vs. platform abstraction (Roc).** Both work, but both pay a tax: features must either be expressible on every target (constraining the language) or live behind conditional FFI (fragmenting the source).
- **Haxe is the most direct analog to Waterfall and the most important case study.** It compiles to JavaScript, C++, C#, Java, Python, Lua, PHP, and HashLink; it has survived 20 years; it has commercial production use (Dead Cells, Northgard). But it is firmly niche — ~6.9k GitHub stars, mostly known as a game-dev language. It demonstrates that surviving is possible but does not produce mainstream legitimacy.
- **The proposed "flow-sensitive readonly statement that freezes an existing mutable binding mid-scope" appears to be largely unexplored in mainstream PL design.** Every flow-sensitive system I found either narrows *types* (Kotlin smart casts, TypeScript narrowing) or freezes *objects at runtime* (Object.freeze, Python PEP 351, Java frozen arrays). The specific design — a compile-time statement that takes a previously mutable local binding and makes the rest of its scope statically reject mutation — does not have a clear prior art match. The closest analogs are Java's "effectively final" (rule, not statement; whole-scope, not from-statement-forward) and Rust's pattern of shadowing/rebinding (workaround, not a feature).

---

## Part 1 — Modern General-Purpose Languages

### Summary Table

| Language | Type System | Memory Model | Error Handling | Ecosystem | Adoption Signal | "Modern" Feel From |
|----------|-------------|--------------|----------------|-----------|-----------------|---------------------|
| **Rust** | Strong static, ADTs, traits, lifetimes | Ownership/borrow checker, no GC | `Result<T,E>` + `?` operator | Cargo, crates.io | TIOBE #16 (Apr 2026), 14.8% SO 2025 usage, "most admired" 72% | Borrow checker, ADTs+match, no nulls, fearless concurrency |
| **Swift** | Strong static, ADTs, protocols | ARC + value semantics + COW | `throws`/`try`, `Result<T,E>`, optionals | SwiftPM, Apple-tier docs | Cross-platform expansion in 6.3 (Mar 2026) — Android, Windows, embedded | Value types, optionals as ADTs, structured concurrency, progressive disclosure |
| **Kotlin** | Strong static, null-safe, smart casts, sealed types | JVM GC (Native: ARC) | Exceptions; `Result<T>` available; flow-sensitive null checks | Maven/Gradle, JetBrains-tier tooling | ~70% of top-1000 Play Store apps; KMP in production at Google Docs, Duolingo, AWS | `val`/`var`, smart casts, null safety, KMP |
| **Go** | Strong static, structural interfaces, generics (1.18+) | GC, goroutines | Multi-return `(T, error)` | go.mod, std-lib-heavy | TIOBE top 10; 91% dev satisfaction | Goroutines/channels, simplicity, fast compile |
| **Zig** | Strong static, comptime generics | Manual allocators (passed explicitly) | Error union types `!T` | Built-in build system | TIOBE #39 (~0.31%); 0.16.0 released Apr 2026 | comptime, explicit allocators, no hidden control flow |
| **Gleam** | Strong static, ADTs, Hindley-Milner | BEAM (Erlang VM) or JS GC | `Result(a, e)`, no exceptions | hex.pm shared with Elixir/Erlang | 21.4k GitHub stars, 8.4k Discord, v1.16.0 (Apr 2026), SO 2025 "most admired" #2 (70%) | Friendly errors, pipe operator, BEAM concurrency, JS interop |
| **Crystal** | Strong static, global type inference, union types | GC | Exceptions (Ruby-style) | shards.info | v1.20 (2026), production-grade | Ruby syntax + static types, macros for metaprogramming |
| **Nim** | Strong static, gradual typing | GC (multiple options), ARC/ORC | Exceptions + `Result` | nimble | NimConf 2026 active community | UFCS, AST macros, multi-target compilation |

### Experimental data points

| Language | Status | Notable |
|----------|--------|---------|
| **Hare** | Active, niche, ~12 maintainers (Feb 2026) | C-like, simple, no UB by design |
| **Roc** | Pre-1.0 (alpha as of May 2026, target 0.1.0 in 2026) | Platform abstraction; purity inference replacing `Task` |
| **Vale** | Development on hold | Linear types + generational references; "Higher RAII" |
| **Carbon** | Pre-MVP, 0.1 maybe late 2026, 1.0 after 2028 | C++ successor, 33.7k stars |
| **Mojo** | Pre-1.0, 50k community, open-source planned fall 2026 | Python-superset feel for AI/GPU; 12x speedup claims |

Sources: [Rust Wikipedia](https://en.wikipedia.org/wiki/Rust_(programming_language)), [Rust 2026 critical](https://www.techtarget.com/searchapparchitecture/tip/Why-is-Rust-a-critical-programming-language), [Swift 2026 cross-platform](https://www.programming-helper.com/tech/swift-2026-cross-platform-android-embedded-systems), [Swift March 2026 update](https://www.swift.org/blog/whats-new-in-swift-march-2026/), [Kotlin 2026 multiplatform](https://medium.com/@amin-softtech/kotlin-multiplatform-in-2026-one-codebase-many-targets-0c0d7cdbfbed), [Kotlin 2.3.20 SWC](https://www.infoworld.com/article/4151375/kotlin-2-3-20-harmonizes-with-c-javascript-typescript.html), [Zig 0.16 release](https://ziglang.org/download/0.16.0/release-notes.html), [Zig 2026 status](https://www.programming-helper.com/tech/zig-programming-language-2026), [Go generic methods 2026](https://www.theregister.com/software/2026/03/02/generic-methods-approved-for-go-devs-miss-other-features/4357203), [Go 1.26 release](https://go.dev/doc/go1.26), [Gleam 2026](https://www.programming-helper.com/tech/gleam-programming-language-2026-type-safe-erlang-beam), [Gleam stars](https://github.com/gleam-lang/gleam), [Crystal 1.20](https://crystal-lang.org/), [Nim Wikipedia](https://en.wikipedia.org/wiki/Nim_(programming_language)), [Hare 2026](https://medium.com/@karacvonthweatt/the-fresh-wave-new-programming-languages-shaking-up-2026-98b15b76a8b3), [Roc plans](https://www.roc-lang.org/plans), [Carbon roadmap](https://docs.carbon-lang.dev/docs/project/roadmap.html), [Mojo Wikipedia](https://en.wikipedia.org/wiki/Mojo_(programming_language)), [SO 2025 survey](https://stackoverflow.blog/2025/08/01/diving-into-the-results-of-the-2025-developer-survey/).

### Lessons for Waterfall — what makes a language *feel* modern in 2026

Three observations from the modern PL set, written as findings (not recommendations):

1. **Sum types / discriminated unions and exhaustive pattern matching are now table stakes.** Rust, Swift, Kotlin (sealed classes), Gleam, Crystal, Zig, Roc, Idris all have them in some form. The 2010s "modern" features — generics, lambdas, type inference, null-safety — have moved into the baseline. The current frontier is structured concurrency (Swift, Kotlin, Go), effect tracking (Roc, Koka), and ergonomic error handling without exceptions (Rust's `?`, Gleam's `Result`).

2. **Tooling is treated as a first-class language deliverable, not an afterthought.** Every successful modern language has a recognized package manager (Cargo, SwiftPM, npm-compatible for KMP, go.mod, nimble, shards) and an editor protocol implementation (LSP). Crystal's interpreter was specifically called out as the feature that brought back Rubyists who missed the "save and run" feel. Languages without these are reflexively read as research projects by working developers.

3. **"Friendly errors" are a marketed feature now.** Roc, Gleam, Elm all explicitly position friendly error messages as a key selling point. Rust's compiler errors are frequently cited as a reason to use the language. This is a low-cost differentiator for a small language — small effort, but high signal of "we care about the developer."

4. **Most-admired vs. most-used remains a wide gap.** From [SO 2025 survey](https://survey.stackoverflow.co/2025/technology): Rust 72% admired but only ~14.8% recent extensive use; Gleam 70% admired with a fraction of one percent usage. "Admired" is a marketable signal for a small language to chase, separate from "used".

---

## Part 2 — Transpiled Languages (the meat)

This is the category Waterfall lives in. Detailed per-language entries follow, then a synthesis.

### 2.1 TypeScript → JavaScript

- **Target**: JavaScript (and now indirectly: everywhere JS runs).
- **Pitch**: Optional static typing for JavaScript, gradual adoption, full superset semantics.
- **Adoption**: TypeScript became the most-used language on GitHub in August 2025 ([GitHub blog via InfoQ](https://www.infoq.com/news/2026/03/state-of-js-survey-2025/)). 40% of State of JavaScript 2025 respondents write *exclusively* in TypeScript, vs 6% in plain JS. 78% of professional developers use TypeScript per one industry report. [Stack Overflow blog](https://stackoverflow.blog/2025/08/01/diving-into-the-results-of-the-2025-developer-survey/) shows it on a multi-year upward trajectory.
- **Lessons**:
  - **Superset semantics.** Any valid `.js` is a valid `.ts`. This made adoption *gradient* rather than binary — you could rename a `.js` to `.ts` and start adding annotations file-by-file. CoffeeScript required full conversion.
  - **Tooling-first investment.** Microsoft built editor integration (initially with VS, later VS Code) before the language went mainstream. Anders Hejlsberg specifically credits open development on GitHub from 2014 onward ([Pragmatic Engineer](https://newsletter.pragmaticengineer.com/p/typescript-c-and-turbo-pascal-with)).
  - **Framework cooptation.** Angular adopting TypeScript in 2016 was the inflection point. Once Angular team was paid to maintain types, the network effect kicked in: React adopted, Vue adopted, Next.js / Nuxt / SvelteKit all defaulted to it.
  - **Microsoft credibility.** This is hard to copy. Microsoft poured resources into a language that competed with their own (C#) for years before traction. Anders Hejlsberg, architect of Turbo Pascal, Delphi, C#, was the public face. Indie projects do not have this.
- **Key takeaway for Waterfall**: TypeScript is the goal post but it cannot be replicated. The combination of (a) addressing a felt pain in the dominant language and (b) backing from the OS vendor is rare.

Sources: [State of JS 2025 / InfoQ](https://www.infoq.com/news/2026/03/state-of-js-survey-2025/), [Hejlsberg history](https://newsletter.pragmaticengineer.com/p/typescript-c-and-turbo-pascal-with), [GitHub blog on TS rise](https://github.blog/developer-skills/programming-languages-and-frameworks/typescripts-rise-in-the-ai-era-insights-from-lead-architect-anders-hejlsberg/), [Wikipedia: Anders Hejlsberg](https://en.wikipedia.org/wiki/Anders_Hejlsberg).

### 2.2 CoffeeScript → JavaScript

- **Target**: JavaScript.
- **Pitch**: Cleaner JS syntax — significant whitespace, no semicolons/curly braces, arrow functions, classes, comprehensions. "It's just JavaScript."
- **Adoption arc**: Created 2009 by Jeremy Ashkenas. Peaked early 2010s. Backbone.js and Ruby on Rails integrated/supported it. Dropbox migrated browser code from JS to CoffeeScript in 2012 — then [migrated *back* to TypeScript in 2017](https://dropbox.tech/frontend/the-great-coffeescript-to-typescript-migration-of-2017). Latest release v2.6.1 in October 2021.
- **Why it died**: ES6 (2015) absorbed CoffeeScript's value props: arrow functions, classes, template literals, destructuring, default parameters. Once those landed in vanilla JS, CoffeeScript's compile step was a tax with no upside. TypeScript meanwhile offered *new* value (types) that JS still lacked.
- **Lessons for Waterfall**:
  - **A transpiled language whose value-add is syntactic sugar lives on borrowed time.** If the target evolves quickly, syntax sugar gets absorbed. Pure sugar is a thin moat.
  - **The killing blow was *another transpiler*, not the target language.** TypeScript ate CoffeeScript's user base; both compile to JS, but one offered a semantic upgrade (types) instead of just syntax. This is important: it implies that the way for a new transpiled language to displace a competitor is *semantic value*, not syntactic preference.

Sources: [Diamond IT: Rise and Fall](https://diamond-it.net/blog/26/coffeescript-the-rise-and-fall-of-a-javascript-alternative), [Better Programming: How CoffeeScript Got Forgotten](https://medium.com/better-programming/how-coffeescript-got-forgotten-812328225987), [Dropbox migration](https://dropbox.tech/frontend/the-great-coffeescript-to-typescript-migration-of-2017).

### 2.3 Dart → JavaScript (originally) → native via Flutter

- **Target**: Originally a JS replacement, with a Dart VM Google planned to ship in Chrome. Plan dropped in 2015. Refocused on JS compilation 2015–2018. Dart 2.0 (2018) introduced a sound type system. Dart 2.6 (Nov 2019) added `dart2native` for Linux/macOS/Windows desktop binaries.
- **Pitch (original)**: Cleaner, structured alternative to JavaScript for large web apps.
- **Pitch (current)**: The Flutter language. Mobile + desktop + web from one source.
- **Adoption**: Failed as a JS replacement. Now ~95%+ of Dart usage is Flutter. Flutter itself is a top mobile framework — Dart's adoption is downstream of Flutter's success, not because Dart-the-language won.
- **Lessons**:
  - **A transpiled language can survive a pivot if it finds an unrelated killer app.** Dart-as-JS-killer was a clear failure. Dart-as-Flutter-language is a clear success. The language identity is no longer "we are an alternative to X" — it's "we are the language for Y."
  - **Pivoting is a possibility, not a strategy.** Google had the resources to keep Dart alive while it searched for a niche. Smaller projects don't.

Sources: [Wikipedia: Dart](https://en.wikipedia.org/wiki/Dart_(programming_language)), [TechTarget: Dart still worth learning](https://www.techtarget.com/searchapparchitecture/tip/4-reasons-Dart-is-still-a-language-worth-learning), [DeusInMachina](https://www.deusinmachina.net/p/the-dart-programming-language-is).

### 2.4 Elm → JavaScript

- **Target**: JavaScript (frontend only).
- **Pitch**: Pure functional, ML-style, no runtime exceptions, friendly compiler errors, full TEA (The Elm Architecture).
- **Adoption**: Niche success, then stasis. Latest release 0.19.1 in **October 2019** — no major release in 5+ years. Used in healthcare, fintech, aerospace, AI dashboards — high-reliability domains. Discord and forum activity continues but enterprise adoption stalled.
- **Lessons / failure modes**:
  - **Single-maintainer (Evan Czaplicki) bottleneck.** Even with a soft-fork in the wild and community contributions, the central project has stagnated. Multiple HN threads ([Ask HN: What happened to Elm?](https://news.ycombinator.com/item?id=34746161)) and a [Rakuten retirement post](https://news.ycombinator.com/item?id=35495910) cite Evan's unilateral control and slow release cadence as adoption blockers.
  - **Strict syntactic difference from target is a barrier.** ML syntax was a culture clash for JS developers; Elm explicitly forbids imperative patterns. Productivity hit in the first weeks is the documented complaint.
  - **Hard interop with JS is a blocker for ecosystems.** Elm's ports system intentionally keeps JS at arm's length. That's a strength for purity but a weakness for ecosystem reuse — every JS library needs a hand-written wrapper or port.
- **Takeaway**: Niche success is achievable but has a known ceiling. If the maintainer disappears, the project freezes. Bus factor is a real concern that affects adoption.

Sources: [GraffersID: React vs Elm 2026](https://graffersid.com/react-js-vs-elm-which-is-better/), [Engage Software: Using Elm in 2025](https://engagesoftware.com/posts/using-elm-in-2025/), [Elmcraft: Elm Core Development](https://elmcraft.org/lore/elm-core-development/), [HN: Why and how we retired Elm](https://news.ycombinator.com/item?id=35495910).

### 2.5 PureScript → JavaScript (+ C++11, Erlang, Go backends)

- **Target**: JavaScript primarily; alternative backends for C++11, Erlang, Go.
- **Pitch**: Haskell-like (purely functional, strong type system, type classes) but with seamless JS interop and easier installation than GHCJS/Haste/Fay.
- **Adoption**: Sustained but small. Active community, an established package manager (Spago), docs site (Pursuit), build tool (Pulp). Specialized usage in companies doing typed FP web apps. Not present in mainstream surveys.
- **Lessons**:
  - **Functional purity + dependent-type-adjacent expressiveness draws a small dedicated audience.** Same shape as Elm but more powerful and less prescriptive about architecture.
  - **Better JS interop than Elm.** PureScript lets you reach into JS without ports; this makes it more pragmatic for production work.
  - **Even with better interop, niche.** "PureScript-style FP" is acquired taste; mainstream JS developers don't reach for type classes and free monads.

Sources: [Wikipedia: PureScript](https://en.wikipedia.org/wiki/PureScript), [Slant: PureScript vs Haskell 2026](https://www.slant.co/versus/389/1537/~purescript_vs_haskell), [PureScript repo](https://github.com/purescript/purescript).

### 2.6 ReScript (formerly BuckleScript / ReasonML) → JavaScript

- **Target**: JavaScript.
- **Pitch**: Robustly typed OCaml-derived language with JS-like syntax and lightning-fast compiles. "JavaScript you can be proud of." Produces human-readable JS output.
- **History**: BuckleScript started at Bloomberg in 2016 to compile OCaml→JS. Facebook's ReasonML reskinned OCaml syntax for JS people. The two merged identities into ReScript in August 2020 to consolidate brand.
- **Adoption**: Small-but-real. The rebrand was explicitly motivated by the *adoption barrier* — having three names (OCaml, BuckleScript, Reason) for what was effectively one stack was confusing newcomers. Production use exists but no Dropbox-scale references.
- **Lessons**:
  - **Brand consolidation matters.** Three names for one thing actively suppressed adoption. The rebrand recognized this.
  - **Fast compiles are a real selling point.** ReScript emphasizes compile speed and clean readable JS output — the "you could go back to plain JS by deleting source files" promise is part of the de-risk message for adopters.
  - **OCaml-DNA is hard to hide.** Even with syntax aligned to TypeScript-style generics (`array<string>`), the underlying type system is unfamiliar to most JS developers.

Sources: [ReScript Blog: BuckleScript Rebranding](https://rescript-lang.org/blog/bucklescript-is-rebranding/), [Ersin Akinci: ReScript, ReasonML explained](https://ersin-akinci.medium.com/confused-about-rescript-rescript-reason-reasonml-and-bucklescript-explained-ab4230555230), [Wikipedia: Reason](https://en.wikipedia.org/wiki/Reason_(programming_language)).

### 2.7 Kotlin/JS → JavaScript

- **Target**: JavaScript (and WASM as of Kotlin 2.3.0).
- **Pitch**: Kotlin code running in browsers/Node. Part of Kotlin Multiplatform (KMP) — share code between JVM, Android, iOS, JS, Native.
- **Adoption**: Limited as a standalone target — Kotlin/JS in isolation is rare. Within KMP, it's gaining production traction. JetBrains is moving toward SWC as the underlying transpilation engine in Kotlin 2.3.20+ to delegate JS-version compatibility concerns out of the Kotlin compiler.
- **Lessons**:
  - **A transpilation target embedded in a larger multiplatform story can succeed where a standalone target would not.** Kotlin/JS is valuable specifically *because* it shares semantics with Kotlin/JVM and Kotlin/Native. The shared business-logic layer is the value, not "let's write web apps in Kotlin."
  - **The same point applies to F#/Fable, Scala.js, ClojureScript.** They thrive when the team is already invested in the parent language and wants to extend reach to the web — not as a draw for new developers.

Sources: [Kotlin/JS overview](https://kotlinlang.org/docs/js-overview.html), [Kotlin 2.3.20 release](https://kotlinlang.org/docs/whatsnew2320.html), [InfoWorld on Kotlin 2.3.20](https://www.infoworld.com/article/4151375/kotlin-2-3-20-harmonizes-with-c-javascript-typescript.html).

### 2.8 Scala.js → JavaScript

- **Target**: JavaScript (and increasingly WASM).
- **Pitch**: Full Scala compiling to efficient JS. Type-safe, sharable code with JVM backend.
- **Adoption**: Mature, stable, active (Scala.js 1.21.0 in April 2026). Small but real production user base. Same dynamic as Kotlin/JS: thrives within an existing Scala team, not a primary draw.
- **Lessons**: Same as Kotlin/JS. Targeting JS as an extension of an existing language ecosystem works; targeting JS as the lead value prop is much harder.

Sources: [Scala.js announce 1.21.0](http://www.scala-js.org/news/2026/04/04/announcing-scalajs-1.21.0/), [Scala.js GitHub](https://github.com/scala-js/scala-js).

### 2.9 Haxe → JS, C++, C#, Java, JVM, Python, Lua, PHP, HashLink, Neko (the most direct Waterfall analog)

- **Targets**: A *lot*. JavaScript, C++, C#, Java, JVM, Python, Lua, PHP, Flash (historically), HashLink, Neko.
- **Pitch**: One language, write once, compile to many platforms. Strict static typing. Cross-platform standard library where possible. Conditional compilation for platform divergence.
- **Adoption**:
  - ~6.9k GitHub stars on the main repo (May 2026).
  - ~20-year history (since 2005). Lead designer Nicolas Cannasse has veto on proposals.
  - The Haxe Foundation funds development; offers paid support plans.
  - **Production use case is dominantly game development.** Dead Cells (Motion Twin) and Northgard (Shiro Games) ship using Haxe + Heaps engine. Papers, Please was developed with a Haxe-based engine.
  - Outside game dev, Haxe is rare. Not in TIOBE top 50. Not in Stack Overflow surveys.
- **Why it has survived but not won mainstream**:
  - **Multi-target is a feature but also a curse.** Code that wants to use platform-native libraries has to use conditional compilation (`#if js`, `#if cpp`, etc.) or platform-specific externs. This makes the language feel "of every world but native to none."
  - **The "common subset" std-lib is constraining.** Anything that needs target-specific behavior (concurrency primitives, FFI patterns) ends up living behind compile flags.
  - **It's known as a game-dev language.** Once a language is type-cast, breaking out of the type is hard. Compare CoffeeScript ("the Rails-era frontend lang") or Crystal ("for Rubyists who want speed").
  - **No dominant company champion.** Microsoft funds TS; Google funds Go and (effectively) Dart; JetBrains funds Kotlin. Haxe Foundation operates on donation income.
- **What Haxe got right**:
  - Conditional compilation is well-designed: `#if`, `#elseif`, `#else` with first-class compiler flags and built-in target defines (`target.static`, `target.sys`, `target.utf16`, `target.threaded`).
  - The Reflaxe framework lets you build new compilation targets via macros — Haxe is meta-extensible.
  - Real commercial games shipping is concrete evidence that "transpiled to multiple targets" can produce production-grade software.

Sources: [Haxe homepage](https://haxe.org/), [Haxe conditional compilation](https://haxe.org/manual/lf-condition-compilation.html), [Haxe target defines](https://haxe.org/manual/lf-target-defines.html), [Dead Cells/MCV interview](https://mcvuk.com/development-news/when-we-made-dead-cells/), [Haxe Foundation GitHub](https://github.com/HaxeFoundation), [Wikipedia: Haxe](https://en.wikipedia.org/wiki/Haxe).

### 2.10 Nim → C, C++, Objective-C, JavaScript (multi-target like Haxe)

- **Targets**: Primarily C (default), also C++, Objective-C, JavaScript.
- **Pitch**: Statically typed compiled language with Python-like syntax. Speed of C, expressiveness of Python.
- **Adoption**: Active community (NimConf 2026 scheduled June 20). Small relative to mainstream. Used for tools, CLIs, some games, some web work.
- **Lessons / FFI differences across targets** (per [Nim Backend Integration docs](https://nim-lang.org/docs/backends.html)):
  - **C targets require `NimMain()` initialization**; JS target doesn't.
  - **C requires forward declarations** for FFI functions; JS doesn't.
  - **Memory sharing differs**: JS has automatic GC matching Nim's, so passing objects to JS is trivial. With C, careful ownership/lifetime planning is required.
  - **`importc` is the common FFI primitive across both targets**, but the *behavior* differs — JS imports are runtime symbol lookups; C imports require linker resolution.
- **Takeaway**: Nim demonstrates that *the same source language with multiple targets* requires the developer to deal with target-specific FFI and runtime semantics. There is no clean "write once truly" abstraction at the FFI layer.

### 2.11 F# Fable → JavaScript, TypeScript, Python, Rust, Erlang, Dart

- **Targets**: A *lot* (more than Haxe in count, less commercial use). Started JS-only; expanded.
- **Pitch**: F# anywhere. Same value prop as Kotlin/JS or Scala.js but with multi-target ambition.
- **Adoption**: Fable 5 release candidate in February 2026. Small, sustained community. The JS target is the most stable and used.
- **Lessons**: Same multi-target tradeoffs as Haxe. Adoption is gated by F# adoption, which is itself niche.

Sources: [Fable.io](https://fable.io/), [Fable 5 release candidate](https://fable.io/blog/2026/2026-02-27-Fable_5_release_candidate.html), [Fable repo](https://github.com/fable-compiler/Fable).

### 2.12 ClojureScript → JavaScript

- **Target**: JavaScript (via Google Closure compiler).
- **Pitch**: Clojure for the browser. Persistent data structures, REPL-driven dev, same language as Clojure JVM.
- **Adoption**: Stable, established. Tools like Shadow CLJS, Reagent, re-frame, Fulcro are mature. Babashka (Clojure scripting on GraalVM) has overtaken ClojureScript as #2 Clojure dialect by usage. Production users exist (Nubank, others) but niche relative to React/TS frontend.

Sources: [ClojureScript 1.12 release](https://clojurescript.org/news/2026-05-07-release), [State of Clojure 2025](https://clojure.org/news/2026/02/18/state-of-clojure-2025).

### 2.13 Idris (1, 2) → C, ChezScheme, ...

- **Target**: Multiple via codegen backends (Idris 2 uses Chez Scheme primarily; C and others available).
- **Pitch**: Dependently typed, supports Quantitative Type Theory (linearity + dependence).
- **Adoption**: Research language. Idris 2 is a self-hosted rewrite based on QTT.
- **Lessons for Waterfall**: Idris is way out of scope as a competitor, but is a reference point for how a small language can be intellectually serious. Its QTT-based linearity (multiplicities 0 and 1) is the closest theoretical relative to "manage when/how a binding can be reassigned."

Sources: [Idris 2: QTT in Practice](https://arxiv.org/pdf/2104.00480), [Idris-lang.org](https://www.idris-lang.org/), [Wikipedia: Idris](https://en.wikipedia.org/wiki/Idris_(programming_language)).

### 2.14 Crystal → native (LLVM), but Ruby-like (included for type-system lessons)

- **Target**: Native via LLVM (not technically transpiled — included because the type system + Ruby-syntax approach has lessons).
- **Pitch**: "Ruby with types and native speed." Statically typed with global type inference — most code reads dynamically typed but is compile-time checked.
- **Adoption**: Crystal 1.20 in 2026, considered production-ready by the community. Smaller than Ruby itself but mature.
- **Lessons**:
  - **Global type inference can make a statically typed language *feel* dynamic.** Crystal infers types across the whole program. This makes the migration from Ruby less jarring.
  - **Tradeoff: compile times scale with code size.** Crystal had to add an interpreter mode for faster iteration. This is a real cost of global inference.

Sources: [Crystal homepage](https://crystal-lang.org/), [Why Crystal 10 Years Later](https://serdardogruyol.com/why-crystal-10-years-later-performance-and-joy), [Wikipedia: Crystal](https://en.wikipedia.org/wiki/Crystal_(programming_language)).

### 2.15 Gleam → BEAM + JavaScript (multi-target)

- **Targets**: BEAM (Erlang VM) and JavaScript. Generates TypeScript definitions for JS interop.
- **Pitch**: Friendly, type-safe, fast on BEAM, runs in the browser. Hindley-Milner inference, ADTs, pipe operator.
- **Adoption**:
  - 21.4k GitHub stars, 8,404 Discord members.
  - v1.16.0 released April 24, 2026 — source map generation added for JS target.
  - 2nd most-admired language on SO 2025 survey (70%) — second only to Rust.
  - Thoughtworks Tech Radar (April 2025) — "Assess" ring.
- **Multi-target handling**:
  - **Conditional compilation** via `@external` annotations: `@external(erlang, "module", "function")` and `@external(javascript, "module", "function")`. Same Gleam function can have two different FFI bodies.
  - The Gleam compiler tracks target support at the *expression level* — if your code only uses target-supported features, you compile cleanly. Mixed-support code is rejected.
  - This makes multi-target a *correctness* property, not a runtime hazard.
- **Lessons for Waterfall**: Gleam is the modern blueprint for "small, friendly, type-safe, multi-target transpiled language." Its expression-level target tracking is more sophisticated than Haxe's macro-style `#if` and arguably the state of the art. Worth deep study.

Sources: [Gleam multi-target v0.34](https://gleam.run/news/v0.34-multi-target-projects/), [Gleam multi-target externals tour](https://tour.gleam.run/advanced-features/multi-target-externals/), [Gleam JavaScript source maps](https://gleam.run/news/javascript-source-maps/), [Gleam 2026 profile](https://www.programming-helper.com/tech/gleam-programming-language-2026-type-safe-erlang-beam).

### 2.16 Roc → various platforms (capability-based)

- **Targets**: Defined per *platform*. A platform is a host runtime (CLI, web server, WASM, GUI, embedded) that Roc apps target.
- **Pitch**: Fast, friendly, functional, no runtime exceptions. Functions are pure by default; effects marked with `!`. Application logic stays pure; platform layer handles I/O.
- **Adoption**: Pre-1.0 (alpha, planning 0.1.0 in 2026). Early adopters exist.
- **Multi-target approach** is fundamentally different from Haxe/Gleam/Nim: **platforms abstract the runtime**. Roc apps target *one platform at a time*; the platform implementer (in Rust or Zig usually) bridges to native primitives. Effects are inferred via purity inference rather than declared via `Task`.
- **Lessons**:
  - **Capability platforms decouple language design from target divergence.** The language doesn't need to know about platform-conditional code; the platform layer is the abstraction boundary.
  - **But this requires the platform layer to be a real artifact.** Each new use case needs a platform written in a system language — not trivial overhead.
  - For a language with the breadth Waterfall has (JS / Python / C), platforms could be one architectural answer to "how do we manage target divergence cleanly."

Sources: [Roc Plans](https://www.roc-lang.org/plans), [Roc FAQ](https://www.roc-lang.org/faq), [Roc Platforms](https://www.roc-lang.org/platforms), [TechTarget: Understanding Roc](https://www.techtarget.com/searchapparchitecture/tip/Understanding-Roc-Functional-and-separate-from-the-runtime).

---

## Part 2.5 — Synthesis: What Makes a Transpiled Language Credible

### The pattern, from the data above

Sort the transpiled languages I researched by approximate credibility-in-2026:

| Tier | Languages | Pattern |
|------|-----------|---------|
| **Won** | TypeScript | Major-vendor backed, gradual adoption, addressed felt pain in dominant language |
| **Found a stable niche** | Haxe (games), Elm (high-reliability frontends), ClojureScript (Clojure shops), Scala.js (Scala shops), Kotlin/JS (KMP shops), F# Fable (F# shops) | Solves a real problem for a defined community, not trying to displace the target |
| **Pivoted to survive** | Dart | Failed at original goal, found Flutter, dominant in unrelated niche |
| **Sustained but small** | PureScript, ReScript, Gleam, Nim, Crystal | Active development, modest user base, no killer product or vendor |
| **Dead/dying** | CoffeeScript | Value prop absorbed by target evolution |

### What credible transpiled languages share

From the case studies above, here's what every transpiled language that *isn't* dead has in common:

1. **A clear, durable semantic value over the target.** Not syntactic — semantic. TypeScript: types JS lacks. PureScript/Elm/Gleam: purity + ADTs JS lacks. Kotlin/JS: shared codebase with JVM Kotlin. CoffeeScript had only syntax. Once ES6 caught up, it died.
2. **A package manager and tool chain.** Cargo, npm, mix/hex.pm for Gleam, Spago for PureScript, nimble for Nim. Without this, the language reads as research.
3. **A documented language reference.** Spec, manual, or comprehensive online docs. CoffeeScript had this; Haxe has this; Elm has this.
4. **At least one well-known production user.** Microsoft uses TS (and itself maintains it); Motion Twin ships Dead Cells in Haxe; Nubank uses Clojure(Script); Flutter (Google) carries Dart. Without a "real software shipped in this" reference, a language reads as a hobby.
5. **Active maintainership.** Elm's 5-year release gap is the cautionary tale. Even with a passionate user base, no releases for 5+ years is read as "abandoned."
6. **Editor/IDE integration.** LSP, syntax highlighting, autocomplete. Without this, modern developers won't try the language seriously.
7. **A target-divergence story.** For multi-target languages (Haxe, Nim, Gleam, F# Fable, Roc), how to handle the parts that can't be the same on every target. Haxe: `#if`. Gleam: `@external`. Roc: platform layer. Nim: pragmas. The strategy varies, but it must exist.

### Why most failed

The "fail mode" categories:

- **Sugar-only value (CoffeeScript)**: target evolves, eats your value.
- **Single-vendor bottleneck (Elm)**: bus factor + slow releases erode trust.
- **Target-attached identity (Kotlin/JS, Scala.js)**: succeed at small scale, plateau without a Microsoft-tier push.
- **Pre-1.0 perpetual (Roc as of 2026, Vale on hold, Carbon)**: development is real but the language isn't widely usable. Most don't reach 1.0.
- **Mass attention without a niche (Carbon, Mojo)**: high awareness, low adoption depth.

### The 2026 "minimum viable legitimate transpiled language" bar

Synthesized from the above, here is the implicit bar a transpiled language must clear in 2026 to be taken seriously by working developers:

**Required:**
- Static type system, type inference, sum types/pattern matching (table stakes).
- LSP/editor support.
- Package manager.
- Public language spec (or detailed manual).
- A clear, *semantic* (not just syntactic) value-add over the target language.
- A documented FFI / interop story with the target.
- Continuous releases (no 5-year gaps).

**Nice-to-have but transformative:**
- A vendor or foundation backing — Microsoft (TS), Google (Go), Apple (Swift), JetBrains (Kotlin).
- A famous shipped-in-production product — Visual Studio Code (TS), Dead Cells (Haxe), Flutter (Dart).
- Friendly error messages.
- Fast compile times.
- WASM target.

**A language that has the "required" set without any of the "nice-to-have" set lives in the "sustained but small" tier.** Gleam is the modern textbook example: it has the required bar, plus friendly errors and clean docs, and is climbing.

### What it means for Waterfall

Waterfall is currently below the "required" bar — there's no package manager, no LSP, no public spec, no production user, no semantic value-add over its targets that I can verify externally. The internal codebase audit (Task #1) will fill in details, but from outside, Waterfall is a transpiler experiment, not a programming language by 2026 community standards.

The path from "transpiler experiment" to "small but credible" is well-trodden: it's roughly the Gleam path. Add a spec, an LSP, a package manager, semantic value-add (types, ADTs, error handling), a target-divergence story, a documented FFI. That gets you onto the "sustained but small" tier. From there, displacing TypeScript or winning the JVM is a separate problem with no proven indie playbook.

---

## Part 3 — Flow-Sensitive Immutability: Prior Art for the Proposed `readonly` Feature

### Recap of the proposed feature

The team-lead's brief describes a `readonly` keyword that:

1. **At declaration time**: marks a binding immutable — `readonly x = 5`. Compile-time enforced. Similar to `const`/`final`/`val`.
2. **As a standalone statement**: `readonly x` on its own line freezes a previously-mutable binding for the rest of its scope. From that point forward, mutation of `x` is a compile error.
3. May extend to subfields: `readonly x.field`.

### Survey of related prior art

#### Declaration-time immutability (well-trodden)

Every modern language has this:

| Language | Keyword | Notes |
|----------|---------|-------|
| Rust | `let` (default) | Mutable explicitly opt-in via `let mut`. Variables shadowable via rebinding. |
| Swift | `let` | Constant binding; for value types, transitively immutable. |
| Kotlin | `val` | Local `val` always smart-cast-friendly. |
| Java | `final` | Plus *effectively final* — the compiler treats unmodified variables as final without the keyword (since Java 8 for lambda captures). |
| Dart | `final` and `const` | `final` = single-assignment runtime; `const` = compile-time constant. `late final` enables deferred initialization. |
| TypeScript | `const` (on top of JS `const`) + `readonly` modifier on properties + `Readonly<T>` utility | Type-level only — disappears in emitted JS. |
| C# | `readonly` (fields/structs/parameters) + `const` | Readonly locals proposed multiple times, [csharplang #3258](https://github.com/dotnet/csharplang/issues/3258), [roslyn #115](https://github.com/dotnet/roslyn/issues/115). Not adopted. |
| C++ | `const` (+ `mutable` escape hatch) | Type qualifier rather than binding modifier. Shallow. |
| F# | `let` (immutable by default), `mutable` opt-in | Reassignment uses `<-`. Shadowing common. |
| Clojure | All bindings immutable | Persistent data structures, no mutable variables. `def` and `let` create fresh bindings. |
| Erlang | All bindings immutable | Single-assignment. |
| OCaml | `let` | Shadowable; explicit `ref` for mutability. |

This is settled territory. The decoration "declare a binding immutable at creation" is a basic feature of every modern statically typed language.

#### Flow-sensitive *narrowing* (well-developed)

This is the closest *concept* to what the proposal describes, but it operates on *types*, not on mutability:

- **Kotlin smart casts**: after `if (x is String)`, `x` is *typed* as `String` in the branch. Crucially, **local `val` smart-casts always work** — Kotlin uses the immutability guarantee to know the type can't change between check and use. **Mutable `var` properties don't smart-cast** because the value could change. ([Baeldung Kotlin: smart cast](https://www.baeldung.com/kotlin/smart-cast-to-type-is-impossible), [Kotlin type checks docs](https://kotlinlang.org/docs/typecasts.html))
- **TypeScript narrowing**: `if (typeof x === 'string')` narrows `x`'s type in the branch. Works on `const`, `let`, parameters, but understands aliasing and reassignment.
- **Ceylon's flow-sensitive typing** (now defunct language): uses union types so `if (input is Integer)` narrows the type. Pure type narrowing; no mutability promotion. ([Wikipedia: flow-sensitive typing](https://en.wikipedia.org/wiki/Flow-sensitive_typing))
- **Dart's definite assignment analysis** + null safety + `late`: handles "is this variable initialized yet" at compile time. Doesn't promote mutability.

#### Promoting *mutability* via control flow (very rare)

This is the part of the proposal that has thin prior art. Let me catalog what I found:

##### Java's "effectively final"

The closest existing concept. A local variable in Java is **effectively final** if it is never reassigned after initialization, even without the `final` keyword. This is the rule the compiler applies when checking lambda captures and inner classes:

```java
int x = 10;       // x is not declared final, but...
useLambda(() -> x); // ...this works because x is never reassigned, so it's effectively final.
```

If you later add `x = 11`, the lambda becomes a compile error.

**Differences from the proposed `readonly` statement:**

- "Effectively final" is **whole-scope** ("x is never reassigned from declaration to scope end"), not **from-this-point-forward**. You cannot say "x was mutable here, but is final from line 7 onward."
- It's a **rule the compiler applies**, not a **statement the programmer writes**. The check is always on; you can't choose to "freeze" mid-scope.
- Its effect is local to lambda/inner-class captures, not a general transferable immutability state.

##### Rust shadowing as a workaround pattern

In Rust, a common pattern is:

```rust
let mut x = build_value();
mutate(&mut x);
let x = x;  // shadows the previous binding; the new `x` is immutable
```

The shadowing rebinds the name to an immutable binding. From this line forward, `x` cannot be reassigned. This is *not* a language feature; it's an idiom. The compiler doesn't have a concept of "promote a binding" — you just bind a fresh immutable name that hides the old one.

[Rust book: variables and mutability](https://doc.rust-lang.org/book/ch03-01-variables-and-mutability.html) mentions this pattern; it's not given a name.

##### Runtime freeze functions (different category)

These are *runtime* mechanisms, not compile-time:

- **JavaScript `Object.freeze(x)`**: prevents addition/removal/modification of properties on `x`. *Shallow*. Throws in strict mode, silent in non-strict. Operates on *objects*, not bindings. ([MDN: Object.freeze](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/freeze))
- **Python PEP 351 `freeze()`** (rejected, but proposed): a freeze protocol where `freeze(obj)` returns an immutable copy. ([PEP 351](https://www.python.org/dev/peps/pep-0351/))
- **Java Frozen Arrays (JEP 8261007, draft)**: a runtime API to convert a mutable array to a frozen array. ([JEP 8261007](https://openjdk.org/jeps/8261007))
- **Kotlin Native `freeze()`** (deprecated): a runtime freeze for Native targets, formerly used to share data across actors. Already removed.

These are conceptually adjacent ("change mutability state at some point") but operate on heap objects at runtime, not on local bindings at compile time. They do not provide what the Waterfall proposal asks for.

##### Idris 2 multiplicities (Quantitative Type Theory)

The most theoretically interesting prior art. In Idris 2, every binding carries a **multiplicity** (0, 1, or ω):

- `0` — erased at runtime, used only at the type level.
- `1` — must be used exactly once (linear).
- `ω` — used freely (the default).

This is part of Quantitative Type Theory. It governs *how often* a binding is used, not *whether* it can be reassigned. But it's the closest theoretical relative to "track binding lifecycle in the type system." Programs that use a binding linearly are effectively single-assignment.

[Idris 2 multiplicities docs](https://idris2.readthedocs.io/en/latest/tutorial/multiplicities.html), [Idris 2: QTT in Practice](https://arxiv.org/pdf/2104.00480).

##### Pony reference capabilities

Pony has 6 reference capabilities including `val` (immutable, shareable), `iso` (isolated/unique), `ref` (mutable, no sharing across actors), `box` (read-only view), `trn` (mutable, but transferable to a `val`/`box`), `tag` (opaque). Capabilities are tracked statically. The `consume` and `recover` keywords allow capability promotion — e.g., recovering a mutable `ref` and consuming it to produce a `val`. ([Pony reference capabilities](https://tutorial.ponylang.io/reference-capabilities/reference-capabilities.html))

This is the *most direct theoretical analog* to what Waterfall proposes. Pony's `recover` block takes a sequence of statements operating on isolated mutable data, then produces a result with a stronger (immutable) capability. The mutable → immutable promotion is at the *block boundary*, not via a single-line freeze statement, but the underlying idea is closely related.

##### C# readonly-locals proposals (never adopted)

The C# language team has discussed adding `readonly` for local variables multiple times. The C# proposals — [csharplang #3258](https://github.com/dotnet/csharplang/issues/3258), [roslyn #115](https://github.com/dotnet/roslyn/issues/115) — discuss `readonly` as a local modifier, not as a mid-scope statement. They've never been merged. One stated concern: "we worry that by introducing a verbose modifier that many people would like to be the default, it will become the new 'thing to do' on every method definition" — a maintainability argument.

##### What no language seems to have

The proposal is for `readonly x` as a **standalone statement on its own line** that takes an existing mutable binding and statically freezes it from that point forward in the same scope.

Searches and language docs do not surface this exact construct in mainstream PL design:

- Not in Rust (uses shadowing pattern).
- Not in Kotlin (uses smart casts on types, not bindings).
- Not in C# (proposed but for declaration only).
- Not in Java (rule-based, whole-scope).
- Not in TypeScript (uses `readonly` for properties only; no flow-sensitive local).
- Not in Swift / Crystal / Gleam.
- Not in Haxe / Nim (not surfaced in their docs).
- Closest: Pony's `recover` block (different ergonomics; block-scoped) and Rust's shadowing pattern (different mechanism; idiom not feature).

**My honest assessment**: the construct as described — flow-sensitive immutability promotion via a single-line statement — appears to be novel in the sense that I can find no language that has this exact syntactic surface and exact semantics. However, the *concept* of "promoting a binding's mutability state across control flow" is well-developed under different names (capabilities, multiplicities, smart casts on type, effectively final). Pony's `recover` is the deepest theoretical work in adjacent territory.

### Gaps and what could not be verified

- I could not find a single shipped language where `readonly` (or any keyword) is a standalone statement that promotes a mutable binding to immutable mid-scope. If this exists, it is in an obscure or unreleased language.
- I did not find published academic work on "flow-sensitive immutability for local bindings" specifically (as opposed to flow-sensitive *typing*). This may be because the construct hasn't been seen as needing dedicated study — it's adjacent enough to capabilities and shadowing that it gets subsumed.
- Specifically for the `readonly x.field` (subfield freezing) variant: this looks even more novel. JavaScript's `Object.freeze` does object-level freezing at runtime. Closest static analog is Pony's `iso`/`val` capabilities transitively applying to fields.

### Honest assessment for the architect

The Waterfall `readonly` proposal is largely novel as a *syntactic feature*, with adjacent prior art in capabilities (Pony), QTT multiplicities (Idris 2), effectively-final (Java), and shadowing idioms (Rust). The novelty is meaningful but should be approached carefully:

- The good: a fresh syntactic primitive can be a clear differentiator for a small language. "Waterfall lets you freeze a variable mid-function, statically" is a sentence-level pitch.
- The risk: there are reasons that mainstream languages haven't done this. The Pony `recover` block, the Rust shadowing idiom, and Java effectively-final each cover important slices of the use case without needing a dedicated freeze statement. The proposal would need to make a clear case why those alternatives aren't enough.
- Specific semantic concerns the strategist/architect should think through (not my call, but flagging as findings):
  - **Branches**: if `x` is frozen in one branch of an `if`, what's its status at the join?
  - **Loops**: can `readonly x` appear inside a loop body? Does the freeze persist across iterations?
  - **Aliasing**: if `x` is a reference (object handle), does freezing `x` prevent the aliased object from mutating? (Pony's capabilities solve this; JS's `Object.freeze` only handles shallow.)
  - **Closures / captures**: when `x` is captured by a closure that runs after the `readonly x` statement, what's the captured value's status?
  - **Subfields**: `readonly x.field` raises questions about what counts as "field" — direct property only, or transitively?

These are the questions other languages' designs answer; Waterfall's design will need answers too.

---

## Part 4 — Recommendations for Waterfall's Positioning

These are observations, not directives — the strategist decides.

**Three plausible positioning niches based on existing landscape gaps:**

1. **"The friendly Haxe": small, multi-target, modern type system, better ergonomics than Haxe.** Haxe occupies the "compiles to many targets" niche but is positioned as a game-dev tool with old-school OOP feel. There's room for a language that occupies the same architectural niche (transpile to JS, C, Python) but with the polish of Gleam — friendly errors, ADTs, modern syntax, fast compiles. The risk: Haxe's niche is small for a reason; multi-target is a structural constraint, not a feature.

2. **"The educational transpiler": a language designed to teach people about transpilation and language implementation by being legible and small.** This pivots away from competing for production use and instead positions Waterfall as a learning tool. Possibly augmented with WebAssembly target. The risk: educational positioning has limited monetization and the bar for "good educational tool" includes a curriculum, examples, and a teaching approach beyond just the language.

3. **"The single-target experimental sandbox":** focus on one target (almost certainly JS), drop multi-target ambitions, position Waterfall as a TypeScript-adjacent experiment with the specific flow-sensitive `readonly` feature as the unique value-add. This is the narrowest position but has the cleanest comparison story ("like TypeScript but with this one feature"). The risk: TypeScript is a moving target and any "novel" feature can be approximated with TS features eventually.

What none of these positions does, and what no realistic indie-built transpiled language can do, is "displace TypeScript" or "be the next big language." Those outcomes require vendor backing (Microsoft, Google, Apple, JetBrains) and 5+ years of investment. Every successful indie transpiled language above (Gleam, Crystal, Nim, PureScript, ReScript) operates in the "sustained but small" tier. That's the realistic ceiling without vendor backing.

The single most actionable observation across all this research: **Gleam is the clearest existing template for a small, credible, modern transpiled language.** Its trajectory, tooling decisions, multi-target story (expression-level target tracking), and friendly-positioning are worth deep study for the strategist's roadmap.

---

## Appendix A — Sources

A curated list of the most useful sources cited above, grouped by topic.

### Modern PL landscape
- [SO 2025 Survey results](https://stackoverflow.blog/2025/08/01/diving-into-the-results-of-the-2025-developer-survey/)
- [TIOBE Index April 2026 (overview)](https://www.tiobe.com/tiobe-index/)
- [TIOBE Index May 2026 commentary](https://www.techrepublic.com/article/news-tiobe-index-language-rankings/)
- [State of JavaScript 2025 / InfoQ](https://www.infoq.com/news/2026/03/state-of-js-survey-2025/)

### Rust
- [Rust Wikipedia](https://en.wikipedia.org/wiki/Rust_(programming_language))
- [Rust references and borrowing (book)](https://doc.rust-lang.org/book/ch04-02-references-and-borrowing.html)
- [Rust variables and mutability (book)](https://doc.rust-lang.org/book/ch03-01-variables-and-mutability.html)

### Swift
- [Swift 2026 cross-platform expansion](https://www.programming-helper.com/tech/swift-2026-cross-platform-android-embedded-systems)
- [Swift March 2026 release notes](https://www.swift.org/blog/whats-new-in-swift-march-2026/)

### Kotlin
- [Kotlin Multiplatform 2026](https://medium.com/@amin-softtech/kotlin-multiplatform-in-2026-one-codebase-many-targets-0c0d7cdbfbed)
- [Kotlin 2.3.20 SWC transpilation](https://www.infoworld.com/article/4151375/kotlin-2-3-20-harmonizes-with-c-javascript-typescript.html)
- [Kotlin smart cast docs](https://kotlinlang.org/docs/typecasts.html)
- [Baeldung Kotlin smart cast](https://www.baeldung.com/kotlin/smart-cast-to-type-is-impossible)

### Go
- [Go 1.26 release notes](https://go.dev/doc/go1.26)
- [The Register: generic methods 2026](https://www.theregister.com/software/2026/03/02/generic-methods-approved-for-go-devs-miss-other-features/4357203)

### Zig
- [Zig 0.16.0 release notes](https://ziglang.org/download/0.16.0/release-notes.html)
- [Zig 2026 profile](https://www.programming-helper.com/tech/zig-programming-language-2026)
- [Zig Wikipedia](https://en.wikipedia.org/wiki/Zig_(programming_language))

### Gleam
- [Gleam homepage](https://gleam.run/)
- [Gleam multi-target externals](https://tour.gleam.run/advanced-features/multi-target-externals/)
- [Gleam multi-target projects v0.34](https://gleam.run/news/v0.34-multi-target-projects/)
- [Gleam 2026 profile](https://www.programming-helper.com/tech/gleam-programming-language-2026-type-safe-erlang-beam)

### Crystal
- [Crystal homepage](https://crystal-lang.org/)
- [Crystal Wikipedia](https://en.wikipedia.org/wiki/Crystal_(programming_language))

### Nim
- [Nim Backend Integration](https://nim-lang.org/docs/backends.html)
- [Nim Wikipedia](https://en.wikipedia.org/wiki/Nim_(programming_language))

### TypeScript
- [State of JavaScript 2025 / InfoQ](https://www.infoq.com/news/2026/03/state-of-js-survey-2025/)
- [GitHub blog: TS rise](https://github.blog/developer-skills/programming-languages-and-frameworks/typescripts-rise-in-the-ai-era-insights-from-lead-architect-anders-hejlsberg/)
- [Pragmatic Engineer: Hejlsberg](https://newsletter.pragmaticengineer.com/p/typescript-c-and-turbo-pascal-with)
- [TypeScript readonly docs / 2ality 2025](https://2ality.com/2025/02/typescript-readonly.html)

### CoffeeScript
- [Diamond IT: Rise and Fall](https://diamond-it.net/blog/26/coffeescript-the-rise-and-fall-of-a-javascript-alternative)
- [Better Programming: How CoffeeScript Got Forgotten](https://medium.com/better-programming/how-coffeescript-got-forgotten-812328225987)
- [Dropbox migration to TypeScript 2017](https://dropbox.tech/frontend/the-great-coffeescript-to-typescript-migration-of-2017)

### Dart
- [Wikipedia: Dart](https://en.wikipedia.org/wiki/Dart_(programming_language))
- [TechTarget: Dart still worth learning](https://www.techtarget.com/searchapparchitecture/tip/4-reasons-Dart-is-still-a-language-worth-learning)
- [Dart null safety understanding](https://dart.dev/null-safety/understanding-null-safety)

### Elm
- [Wikipedia: Elm](https://en.wikipedia.org/wiki/Elm_(programming_language))
- [Elmcraft: Elm Core Development](https://elmcraft.org/lore/elm-core-development/)
- [HN: Why and how we retired Elm](https://news.ycombinator.com/item?id=35495910)
- [HN: What happened to Elm?](https://news.ycombinator.com/item?id=34746161)

### PureScript
- [PureScript homepage](https://www.purescript.org/)
- [Wikipedia: PureScript](https://en.wikipedia.org/wiki/PureScript)
- [Slant: PureScript vs Haskell](https://www.slant.co/versus/389/1537/~purescript_vs_haskell)

### ReScript / BuckleScript / Reason
- [ReScript: BuckleScript Rebranding announcement](https://rescript-lang.org/blog/bucklescript-is-rebranding/)
- [Ersin Akinci: ReScript history explained](https://ersin-akinci.medium.com/confused-about-rescript-rescript-reason-reasonml-and-bucklescript-explained-ab4230555230)
- [Wikipedia: Reason](https://en.wikipedia.org/wiki/Reason_(programming_language))

### Kotlin/JS, Scala.js, F# Fable, ClojureScript
- [Kotlin/JS overview](https://kotlinlang.org/docs/js-overview.html)
- [Scala.js 1.21.0 announcement](http://www.scala-js.org/news/2026/04/04/announcing-scalajs-1.21.0/)
- [Fable.io](https://fable.io/)
- [ClojureScript 1.12 release](https://clojurescript.org/news/2026-05-07-release)
- [State of Clojure 2025](https://clojure.org/news/2026/02/18/state-of-clojure-2025)

### Haxe
- [Haxe homepage](https://haxe.org/)
- [Haxe conditional compilation manual](https://haxe.org/manual/lf-condition-compilation.html)
- [Haxe target defines](https://haxe.org/manual/lf-target-defines.html)
- [Dead Cells / MCV interview](https://mcvuk.com/development-news/when-we-made-dead-cells/)
- [Wikipedia: Haxe](https://en.wikipedia.org/wiki/Haxe)

### Idris / linear / capability languages
- [Idris 2: QTT in Practice (paper)](https://arxiv.org/pdf/2104.00480)
- [Idris 2 multiplicities](https://idris2.readthedocs.io/en/latest/tutorial/multiplicities.html)
- [Pony reference capabilities tutorial](https://tutorial.ponylang.io/reference-capabilities/reference-capabilities.html)
- [Pony recovering capabilities](https://tutorial.ponylang.io/reference-capabilities/recovering-capabilities.html)
- [Substructural type system Wikipedia](https://en.wikipedia.org/wiki/Substructural_type_system)
- [Koka homepage](https://koka-lang.github.io/koka/doc/book.html)

### Roc, Hare, Vale, Carbon, Mojo
- [Roc plans](https://www.roc-lang.org/plans)
- [Roc FAQ](https://www.roc-lang.org/faq)
- [Hare homepage](https://harelang.org/)
- [Vale homepage](https://vale.dev/)
- [Carbon roadmap](https://docs.carbon-lang.dev/docs/project/roadmap.html)
- [Mojo Wikipedia](https://en.wikipedia.org/wiki/Mojo_(programming_language))

### Flow-sensitive typing / immutability
- [Flow-sensitive typing Wikipedia](https://en.wikipedia.org/wiki/Flow-sensitive_typing)
- [Effectively final Java rule](https://www.baeldung.com/java-effectively-final)
- [Baeldung: effectively final lambdas](https://www.baeldung.com/java-lambda-effectively-final-local-variables)
- [csharplang #3258: Allow local readonly](https://github.com/dotnet/csharplang/issues/3258)
- [roslyn #115: readonly for locals and parameters](https://github.com/dotnet/roslyn/issues/115)
- [TS readonly modifier (2ality)](https://2ality.com/2025/02/typescript-readonly.html)
- [MDN: Object.freeze](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/freeze)
- [Python PEP 351 (rejected) freeze protocol](https://www.python.org/dev/peps/pep-0351/)
- [OpenJDK JEP 8261007: Frozen Arrays draft](https://openjdk.org/jeps/8261007)
- [Dart late and final docs](https://sarunw.com/posts/initialize-final-variables-in-dart/)

---

## Appendix B — What I could not verify

- **Exact PureScript and ReScript user base counts.** GitHub stars and Discord membership were not surfaced in searches.
- **Exact Haxe maintainer count.** ~12 known core team members estimated, not confirmed.
- **Whether there exists a research language with mid-scope `readonly` promotion.** I searched general PL design literature and major language repos; no match. There may be obscure or unreleased work I missed.
- **Whether the Pony / Idris-2 designs are *known* to the Waterfall designer.** Worth a separate discussion — the prior-art question is partly a "what's novel here" question and partly a "what to learn from" question.
- **Specifics of how Haxe's adoption trajectory has changed in the last 2 years.** The community appears stable, but I couldn't find a recent state-of-Haxe survey.
