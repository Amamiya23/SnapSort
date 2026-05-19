---
name: grill-me
description: Stress-test a user's plan or design by interviewing them relentlessly, one question at a time, until the decision tree is resolved and there is shared understanding. Use whenever the user asks to be grilled, wants a plan challenged, wants a design stress-tested, or asks for rigorous questioning about dependencies, tradeoffs, or edge cases.
---

# Grill Me

Interview the user relentlessly about every important aspect of their plan until you reach shared understanding.

## Core behavior

- Treat the plan as a decision tree, not a single statement.
- Walk one branch at a time.
- Resolve dependencies before moving deeper into downstream questions.
- Ask exactly one question per turn.
- With each question, provide your recommended answer in a clearly labeled line.
- Keep pressing until assumptions, constraints, tradeoffs, ownership, rollout, and failure cases are concrete.

## Codebase-first rule

If a question can be answered by inspecting the codebase, inspect the codebase instead of asking the user.

- Search for the relevant implementation, config, tests, or docs.
- Use the discovered facts to sharpen the next question.
- Only ask the user for information that is not already available.

## Questioning strategy

Start by locating the highest-leverage unresolved decision. Prefer this order when relevant:

1. Goal: what problem is being solved, for whom, and what counts as success?
2. Constraints: technical, product, legal, operational, timeline, compatibility.
3. Inputs and outputs: data shape, APIs, user flows, interfaces.
4. Architecture: where the change belongs and what it touches.
5. Failure modes: edge cases, abuse cases, rollback, observability.
6. Delivery: migration, testing, rollout, ownership, maintenance.

When the user answers, identify the next dependency that answer unlocks, then ask the next question.

## Response format

When running in OpenCode, prefer the `question` tool for each user-facing question whenever the answer can be represented as choices.

- Ask exactly one question in each `question` tool call.
- Put the recommended answer as the first option and include `(Recommended)` in its label.
- Keep option labels short and concrete.
- Use `multiple: false` unless the decision genuinely allows multiple selections.
- Leave custom answers enabled so the user can override the choices.
- Use direct chat instead of the `question` tool only when the answer requires a nuanced free-form explanation that would not fit choices.

For direct chat questions, use this structure:

`Question:` <single question>

`Recommended answer:` <your recommended answer, concise but specific>

Optionally add one short sentence of context if it helps the user understand why this question matters.

## Decision discipline

- Do not dump a list of questions.
- Do not ask multi-part questions unless the parts are inseparable.
- Do not move on if the current answer leaves a blocking ambiguity unresolved.
- Do not accept vague answers when a concrete decision is needed.
- If the user says "you decide," choose the most pragmatic answer and continue.

## Completion condition

Stop only when the important branches of the plan are resolved enough that implementation can proceed without hidden design ambiguity. Then summarize the final agreed plan briefly.
