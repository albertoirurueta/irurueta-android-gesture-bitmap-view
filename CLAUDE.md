# Contributor conventions

Android library (`lib/`) with a sample app (`app/`), written in Kotlin and built with Gradle.
These conventions apply to every change, in main and test code.

## Member ordering

Order the members of a class or file by kind and visibility, the standard way in Kotlin and Java:

1. Properties: public first, then internal, then private.
2. Initializer blocks and secondary constructors.
3. Lifecycle and overridden methods (in tests: `@Before`, `@After`).
4. Public methods (in tests: the `@Test` methods).
5. Internal methods, then private methods.
6. Nested and inner classes, interfaces and enums.
7. The companion object, last.

Private methods, helper classes and other nested types always go at the end of the file. Never
place a private helper between the setup and the tests, or between two public methods.

## Test naming

Name every test method `function_condition_expectedResult`:

- `function`: the method, property or behaviour under test (`setImageSource`, `constructor`).
- `condition`: the state or input that matters, starting with `with` or `when`. Omit it only
  when there is none.
- `expectedResult`: what the test asserts (`closesPrevious`, `returnsExpectedValue`).

Examples: `constructor_withAttributeSet_initializesWithDifferentParameters`,
`invalidateImage_whenFileIsRewritten_keepsImageSource`.

## File size

Keep files small and focused on one responsibility.

- Do not grow a large file further. When a change adds a new concern to a class, put that
  concern in its own class and file, and have the existing class delegate to it. For example the
  drawing of images set through an `ImageSource` lives in `SourceImageRenderer`, not in
  `GestureBitmapView`.
- Prefer extracting and reusing functionality over adding private members to an existing class.
  Smaller classes are easier to read, review and test on their own.
- As a guide, plan a split when a new source file would pass about 500 lines, and do not add
  more than a small amount of glue code to a file that is already larger than that.
- Each extracted class gets its own test class.

## Other rules

- Every source and test file starts with the license header used across the repository.
- Every class, method, property and constant has KDoc, including private ones.
- Code examples in `docs/modules/ROOT/examples` are copies of the tagged regions of
  `DocumentationExamplesTest`, which fails when they differ. Change both together.
