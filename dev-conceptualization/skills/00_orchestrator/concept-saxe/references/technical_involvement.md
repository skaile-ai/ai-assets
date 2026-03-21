# Technical Involvement Protocol

When a skill needs to make technical decisions (architecture, data model, tech
stack integrations), it first asks the user whether they want to be involved
in the details or let the agent handle it automatically.

## Involvement Modes

### Automatic Mode

The agent analyzes features and context, makes best-judgment decisions, and
presents a business-language summary for lightweight approval.

**Workflow:**
1. Read all input artifacts (features, brief, behavior specs)
2. Analyze requirements and make decisions without asking user
3. Write the output artifacts
4. Present a summary in business terms:
   > "Here's what I decided: [plain-language summary]. Approve, or tell me what to change."
5. On approval, proceed. On feedback, iterate.

### Involved Mode

Current behavior — the skill asks detailed questions before making decisions.

**Workflow:**
1. Read all input artifacts
2. Ask the user skill-specific questions (architecture: 5 questions, datamodel: 3 questions, techstack: integration categories)
3. Write output artifacts based on answers
4. Present summary and await approval

## Tier Defaults

| Tier | Default mode | Prompt |
|------|-------------|--------|
| small | automatic | "I'll handle this automatically based on your features. I'll show you a summary when done. Want to review the details instead?" |
| standard | automatic (offer involvement) | "Would you like to be involved in the [technical area] design, or should I handle it based on your features?" |
| complex | involved (recommend) | "The [technical area] is a key decision point for your app. I recommend we go through the design together. Or I can propose something and you review?" |

## Business-Language Summary Pattern

When presenting decisions in automatic mode, use this pattern:

1. Lead with what the user's app **can do** as a result
2. Describe relationships and data in **natural language** (not entity/model terminology)
3. Mention any **external connections** in terms of what they enable
4. Offer technical details as an optional drill-down

**Example (architecture):**
> "Your app is set up to handle [primary capability]. [If real-time]: Features like [feature] will show updates instantly. [If integrations]: It connects to [service] for [purpose]."

**Example (data model):**
> "Your app will keep track of Events, Sessions, and Speakers. Each event can have multiple sessions, and speakers are assigned to sessions. Users can register for events and get attendance confirmations."

**Example (tech stack):**
> "Your app is built on a proven production stack — fast, secure, and ready to scale. [If integrations]: It connects to Stripe for payments and SendGrid for email notifications."
