# Waterfall

### Setup

1. Install JavaScript dependencies: `yarn install`
2. Install Java dependencies and build the jar: `./gradlew build`

### Run

* Show usage information: `java -jar build/libs/waterfall-0.0.1.jar --help`
* Compile file(s): `java -jar build/libs/waterfall-0.0.1.jar <file1> <file2> ...`

### Syntax Design

Assignments: `=`, `:=`
Operators: `/`, `*`, `+`, `-`, `%`
Primitive Types: `int`, `dec`, `bool`, `char`
Control: `if`, `elif`, `else`, `for`, `while`, `in`
Conditionals / Comparators: `and`, `or`, `equals`, `<`, `>`, `<=`, `>=`
Modifiers: `const`, `final`
Functions: `func`, `returns`
Containers: `module`, `type`
Casting: `castas`
Return: `return`

### Examples

**Find the sum of all the multiples of 3 or 5 below 1000.**
```
int sum = 0
for(int num = 1; num < 1000; num++) {
  if(sum % 3 equals 0 or sum % 5 equals 0) {
    sum += num
  }
}
print(sum)
```

**Determine whether a string can be formed from the characters in another.**
```
func canBeFormed(char[] characters, char[] word) returns bool {
  const asciiOffset := 97
  const letterCounts := int[].create(26)

  for(char c in characters) {
    const charVal := c castas int
    letterCounts[charVal - asciiOffset]++
  }

  for(char c in word) {
    const charVal := c castas int
    letterCounts[charVal - asciiOffset]--
    if(letterCounts[charVal - asciiOffset] < 0) {
      return false
    }
  }

  return true
}
```
