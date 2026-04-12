Refine and redesign the PostForge web app UI as a coherent, launch-ready Korean financial insight product.

Product:
- PostForge is a Korean stock analysis community + AI research platform
- Users read AI and human-written market analysis, inspect evidence and sources, discuss in comments, generate AI analysis, and write their own posts
- The product should feel trustworthy, analytical, editorial, premium, and calm

Main correction goal:
The current result has good intent, but still feels inconsistent and slightly unstable as a real product UI.
It needs to become more coherent, more usable, and more polished across layout, spacing, interaction states, and page structure.

Important problems to fix:

1. Horizontal layout shift on button interaction
There is a subtle rightward movement or layout jank when clicking or activating buttons such as:
- AI생성
- 글쓰기
- 로그인
- 회원가입
- AI분석
- AI분석생성
- 상승신호
- 하락신호

This must be fixed.
Requirements:
- Lock the overall page content width and left/right gutters consistently
- Keep horizontal padding fixed across the full app
- Audit button, tab, chip, and interactive component padding so hover, active, focus, selected, and pressed states do not change width or push surrounding layout
- Do not use border changes, padding changes, or font-weight changes that cause layout shift
- Use color, background, shadow, inset ring, or opacity changes instead of size-changing interaction feedback
- Ensure tabs, nav buttons, and pill filters remain perfectly stable when selected

2. Add proper light mode and dark mode
Implement both light mode and dark mode.
Requirements:
- Both modes must feel intentional and premium
- Do not simply invert colors
- Preserve the PostForge editorial identity in both themes
- Light mode: warm ivory / paper-like background, deep ink text, restrained brass accent
- Dark mode: deep charcoal / ink background, soft warm text, muted brass highlights, elegant contrast
- Keep positive / negative / warning / AI states legible in both modes
- Add a proper theme toggle in the header or top-level navigation
- Maintain consistent component behavior and spacing in both themes

3. Some pages have too much empty space between main content and footer
There are pages where the middle content ends too early and leaves awkward empty space before the footer.
This should be fixed by improving page composition, not just stretching boxes.
Requirements:
- For short pages, add meaningful supporting content in the lower area
- Fill the mid-to-lower page region with useful secondary content such as:
  - related analysis
  - suggested actions
  - AI helper prompts
  - recent activity
  - market highlights
  - account help panels
  - writing tips
  - secondary navigation or recommendations
- Do not leave sparse pages feeling unfinished or abruptly cut off

4. The space between content and footer is too tight
On some pages the content ends and the footer begins too abruptly.
Requirements:
- Increase vertical breathing room above the footer
- Add more intentional bottom spacing after the final content block
- The transition from main content to footer should feel natural and composed
- Keep the footer visually separated from content with enough spacing and hierarchy
- Avoid pages where the main content appears to be cut off directly into the footer

High-level design goals:
- Make the app feel more product-ready and less like a styled mockup
- Reduce decorative over-styling by about 30%
- Reduce excessive uppercase labels and overly wide letter spacing
- Reduce overuse of thick borders and overly sharp buttons
- Keep the editorial intelligence feel, but make it quieter, smarter, and more realistic

Design system requirements:
- Create one coherent design language across homepage, detail page, AI pages, auth pages, profile, and editor
- Shared system for:
  - colors
  - typography
  - spacing
  - cards and surfaces
  - chips / signal badges
  - buttons
  - input fields
  - dividers
  - footer spacing
  - dark/light theming
- Keep left/right page gutters consistent across all pages
- Desktop and mobile should feel aligned, not like separate design systems

Visual direction:
- Tone: analytical, premium, editorial, calm, trustworthy
- Background: warm ivory / off-white in light mode
- Dark mode: deep charcoal with warm contrast
- Typography:
  - serif headlines for authority
  - clean Korean sans-serif for UI and body
- Brass accent should be restrained and used only where it adds emphasis
- Positive and negative signals should be highly scannable without becoming flashy

Pages to refine:

1. Homepage / feed
Requirements:
- stronger but stable hero
- market pulse or signal overview
- featured analysis block
- clear and stable tab/filter system
- information-dense analysis cards
- no horizontal shifting when tabs/buttons are pressed
- meaningful lower-page content before footer

2. Post detail page
Requirements:
- strong article header
- executive summary
- signal summary
- evidence / sources
- key stats
- risk / caution panel
- related analysis
- comments
- generous bottom spacing before footer
- if content is shorter, add supporting modules rather than leaving dead space

3. AI chat / AI analysis / AI generation pages
Requirements:
- make them feel like research workstations, not generic chat tools
- stable action buttons and nav states
- useful lower-page supporting content when the page is short
- clear dark/light mode behavior
- preserve left/right padding consistency

4. Login / Register
Requirements:
- trustworthy and minimal
- stable buttons and auth actions
- consistent content width and padding
- no abrupt page ending above footer
- add supporting lower-page content if needed, such as trust signals, feature summary, or why join

5. New post / editor / profile
Requirements:
- clearer information grouping
- stable tabs/buttons/actions
- better bottom spacing before footer
- if pages are visually short, add useful secondary content
- maintain consistent gutter and content width

Interaction rules:
- No button press, hover, active, selected, or focus state should cause layout movement
- No width changes on tabs or chips when selected
- No subtle rightward jumping caused by border or padding differences
- All states must feel smooth and intentional

Avoid:
- generic SaaS dashboard look
- generic chatbot look
- crypto / hype visuals
- purple gradients
- glassmorphism
- overuse of uppercase labels
- too many framed boxes
- overly sharp rectangular UI everywhere
- decorative styling that harms usability
- empty dead zones above the footer
- layout shift during interaction

Output:
- Create a cohesive, high-fidelity, shippable UI
- Show both light mode and dark mode
- Fix layout stability, footer spacing, and sparse-page composition
- Make the app feel premium, Korean, analytical, restrained, and real
