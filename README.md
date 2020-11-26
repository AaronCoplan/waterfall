# Waterfall

# Considering writing a paper on Waterfall before implementing it

## Code Examples

```jsx
// strings use ` (less escaping this way)
myFruits := [`banana`, `orange`];
myFruits.each(fruit ==> print(fruit));

imm |hasError, returnValue| := returnBundle();
if (hasError) {
    print(`An error occurred!`);
}

// static function calls use ::
sortedFruits := Vec::sort(myFruits);

// object function calls use .
firstSortedFruit := sortedFruits.get(0);

// bundles are typed tuples and use |a,b,c| syntax
func returnBundle() returns |bool, string| {
   return |false, `I am a string`|;
}
```

Default Immutable

```jsx
mut counter := 0
list := [1,2,3]
list.add(4) // ILLLEGAL because list is default immutable
for(item in list) {
   if(item.count > 5) ++counter
}
```

Default Mutable

```jsx
counter := 0
imm list := [1,2,3]
for(item in list) {
	if(item.count > 5) ++counter
}
```

## Notes

Compiler Phases:

1. [DONE] Argument parsing and handling
2. [DONE] Parsing of files and syntax error checking
3. [DONE] Top level symbol table creation
4. [IN PROGRESS] Inline verification and translation using symbol tables
- *Grammar and Parser*
    - Clean grammar design (_ )_ [https://kotlinlang.org/docs/reference/grammar.html](https://kotlinlang.org/docs/reference/grammar.html)
    - Spoon (_ )_ [http://spoon.gforge.inria.fr/code_elements.html](http://spoon.gforge.inria.fr/code_elements.html)
- Global namespacing
    - `print()` etc need to be global
- *Syntactical Sugar*
    - Object destructuring like ES2015
    - Inferred types
    - Operator overriding (Or skip?) operator list (get, set, add, multiply, divide, subtract)
    - [] access notation maps to get() function on right side and set() function on left side
    - Automatic getter/setter syntax (Kotlin), just need to be careful about naming conflicts
    - Using / with for resources like files etc (Gosu, python)
    - Direct c (?) i.e. c(`memcpy(ptr1, ptr2, size)`);
    - Use `` for strings
- *Objects*
    - Fields private be default
    - Jsonify/fromjson
    - Alloc/free
    - No default constructor (wren)
    - All fields private - must write getter and setter, read only properties / variable exposure (gosu)
    - Data classes / objects?
- *Arrays*
    - Bundles for multiple returns (array of void*)
    - Array slicing
- *Modifiers*
    - undefine / Delete / finalize variable
    - Const / final / finalize
    - Local variables immutable by default (?)
- *Import system*
    - Important to think about imports
- *Threads*
    - Important to think about threads
- *Async/Await*
    - [https://docs.hhvm.com/hack/asynchronous-operations/utility-functions](https://docs.hhvm.com/hack/asynchronous-operations/utility-functions)
    - [https://hhvm.com/blog/7091/async-cooperative-multitasking-for-hack](https://hhvm.com/blog/7091/async-cooperative-multitasking-for-hack)
    - [https://github.com/facebookarchive/asio-utilities](https://github.com/facebookarchive/asio-utilities)
    - [https://github.com/facebook/hhvm/tree/master/hphp/test/slow/async](https://github.com/facebook/hhvm/tree/master/hphp/test/slow/async)
    - [https://luminousmen.com/post/asynchronous-programming-cooperative-multitasking](https://luminousmen.com/post/asynchronous-programming-cooperative-multitasking)
    - [https://dev.to/nestedsoftware/is-cooperative-concurrency-here-to-stay-5adb](https://dev.to/nestedsoftware/is-cooperative-concurrency-here-to-stay-5adb)
    - [setcontext - Wikipedia](https://en.wikipedia.org/wiki/Setcontext)
    - [GitHub - Lupus/libevfibers: Small C fiber library that uses libev based event loop and libcoro based coroutine context switching.](https://github.com/Lupus/libevfibers)
- *Type System / Specifications*
    - Nullable types using question mark
    - Object? = Object | null
    - Can objects and modules implement specifications?
- *Error handling*
    - none (Pony)
    - error propagation [Rust](https://doc.rust-lang.org/1.30.0/book/second-edition/ch09-02-recoverable-errors-with-result.html#a-shortcut-for-propagating-errors-the--operator)
    - C++ Examples:
        - [https://www.codeproject.com/Articles/2126/How-a-C-compiler-implements-exception-handling](https://www.codeproject.com/Articles/2126/How-a-C-compiler-implements-exception-handling)
        - [C++ exception handling internals – An infinite monkey – Nico Brailovsky’s blog](https://monoinfinito.wordpress.com/series/exception-handling-in-c/)
- *C FFI (Python, Pony)*
- *Compiler configuration*
    - x := {} becomes x := Map.new() -> make statements like this be configurable if you want it to be a different type ?
    - Configurable language backend? Use your own thread pools ? Your own schedulers? Provide a configurable way for developers to do this without ripping out the backend / modifying waterfall’s implementation?
- *Functions*
    - Arguments must be all positional OR all named
    - Lambdas
    - Callables / Function Pointers
    - Ability to return and pass around functions? (Swift)
    - All function parameters default final?
    - Interface copy-down for static method binding
    - How to make functions public / private