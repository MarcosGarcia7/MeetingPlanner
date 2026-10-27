/** @type {import('stylelint').Config} */
const config = {
  extends: ['stylelint-config-standard'],
  rules: {
    // Colors come from the tokens in globals.css (see Styling in CLAUDE.md).
    'color-no-hex': true,
    'color-named': 'never',
    // CSS Modules use camelCase class names.
    'selector-class-pattern': null,
    'property-no-unknown': [true, {ignoreProperties: ['composes']}],
    'value-keyword-case': ['lower', {ignoreProperties: ['composes']}],
    // Tokens in globals.css are grouped with blank lines.
    'custom-property-empty-line-before': null,
    // Allow placeholder modules for components that aren't styled yet.
    'no-empty-source': null,
  },
  overrides: [
    {
      files: ['src/app/globals.css'],
      rules: {
        'color-no-hex': null,
        'color-named': null,
      },
    },
  ],
};

export default config;
