# Waterfall

### Setup

1. Install JavaScript dependencies: `yarn install`
2. Install Java dependencies and build the jar: `./gradlew build`

### Run

* Show usage information: `java -jar build/libs/waterfall-0.0.1.jar --help`
* Compile file(s): `java -jar build/libs/waterfall-0.0.1.jar <file1> <file2> ...`

### Syntax Design

Operators: `/`, `*`, `+`, `-`, `%`
Primitive Types: `int`, `dec`, `bool`, `char`
Control: `if`, `elif`, `else`, `for`, `while`
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
  int[] letterCounts

  for(char c : characters) {
    int charVal = c castas int
    letterCounts[charVal-97]++
  }

  for(char c : word) {
    int charVal = c castas int
    letterCounts[charVal-97]--
    if(letterCounts[charVal-97] < 0) {
      return false;
    }
  }
  
  return true;
}
```
