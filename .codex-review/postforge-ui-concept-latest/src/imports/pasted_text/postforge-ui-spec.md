Design a high-fidelity web app UI for a Korean financial insight platform called PostForge.

Product:
- PostForge is a Korean stock analysis community + AI research platform
- Users read AI and human-written market analysis, inspect supporting evidence, discuss in comments, and use an AI assistant for research
- The product should feel trustworthy, editorial, analytical, calm, and premium

What is wrong with the current direction:
- It currently feels too decorative and over-styled rather than product-grade
- Too much uppercase text, wide tracking, heavy borders, sharp corners, brass accents, and editorial texture
- The detail page tries too hard to look like a magazine spread and loses reading clarity
- The AI chat looks like a stylish messenger, not a serious research workstation
- Different pages feel like different templates instead of one coherent product
- Some screens are too generic, while others are too theatrical

Your goal:
Create a more refined, more usable, and more product-consistent UI.
It should still feel editorial and premium, but more restrained, more intelligent, and more realistic for a launchable product.

Visual direction:
- Tone: premium, calm, analytical, editorial, trustworthy
- Background: warm ivory / paper-like off-white
- Text: deep ink / charcoal
- Accent: muted brass used sparingly, only for emphasis
- Typography:
  - elegant serif headlines
  - clean Korean sans-serif body text
  - strong but restrained hierarchy
- Use whitespace intentionally, but do not make the UI feel empty
- Use subtle panels, chips, signal badges, dividers, evidence cards, and summary blocks
- Corners should be lightly rounded, not fully sharp and not overly soft
- The result should feel like a Korean market intelligence product, not a design exercise

Important design corrections:
- Reduce decorative styling by about 30%
- Reduce excessive all-caps labels and excessive letter spacing
- Avoid mixing too much English with Korean UI text
- Avoid overusing thick borders and framed boxes everywhere
- Avoid making every section visually loud
- The UI should guide reading and decisions, not show off styling tricks

Must feel like one design system:
- Homepage, detail page, AI chat, auth, profile, and editor must all feel related
- Shared system for colors, spacing, typography, cards, chips, and actions
- Strong contrast between:
  - page background
  - surface cards
  - elevated panels
  - primary text
  - secondary/meta text
  - positive / negative / warning / AI states

Pages to design:

1. Homepage / feed
Goal:
- Help users understand what matters in the market today within 3 seconds
- Emphasize scanability and prioritization

Include:
- strong but restrained hero
- market pulse or today’s signals section
- featured analysis block
- structured tab/filter area
- analysis feed with information-dense cards
- clear distinction between AI-written and human-written posts
- visible positive / negative / neutral signal treatment

Homepage should feel:
- highly scannable
- credible
- more like a financial intelligence feed than a blog

2. Post detail page
Goal:
- Create a reading experience for deep analysis, not a long generic article

Include:
- strong article header
- executive summary block
- signal summary cards
- evidence / sources module
- key market stats
- risk / caution panel
- related analysis section
- comments section
- optional desktop side rail, but make it visually calm and useful

Detail page should feel:
- immersive
- structured
- readable
- premium
- not overcrowded

3. AI chat page
Goal:
- Make it feel like a research desk / analyst workstation, not a chat toy

Include:
- serious header
- suggested prompts
- structured assistant answer blocks
- source/evidence references
- follow-up action suggestions
- clear visual distinction between user input and AI analysis
- room for market insight cards or quick takeaways

AI page should feel:
- useful
- focused
- evidence-aware
- professional

4. Auth screens
Goal:
- clean, trustworthy, minimal, premium
- no playful startup/AI vibe

Include:
- login and register screens
- calm and brand-consistent form hierarchy
- social login buttons that feel integrated
- polished validation and helper text

5. New post / editor
Goal:
- make writing analysis feel intentional and high-quality

Include:
- title
- summary
- body editor
- tag UI
- attachment upload
- publishing actions
- clear information grouping and clean form rhythm

Content/UI requirements:
- All primary UI text should be in Korean
- Use English only when truly necessary
- Make desktop and mobile versions believable
- Use realistic spacing and component sizing
- Keep it implementable in React + Tailwind

Avoid:
- generic SaaS dashboard look
- generic chatbot look
- crypto / fintech hype visuals
- purple gradients
- glassmorphism
- excessive texture
- overuse of all-caps labels
- too many outlined boxes
- overly sharp rectangular UI everywhere
- default shadcn feel
- purely decorative editorial styling that hurts usability

Output:
- Create a cohesive, high-fidelity, launch-ready product UI
- Focus on homepage, post detail, AI chat, auth, and editor
- Make the result feel premium, Korean, analytical, restrained, and shippable
