# Decisions

## Local mechanics catalog

- Decision: use JPA entities with a local H2 database for the first catalog.
- Reason: analysis remains reproducible while reviewed mechanics can be managed as persistent records.
- Alternative: live external lookup. Rejected because game-rule evidence must be reviewed before use.

## Supported PoB input

- Decision: accept raw Path of Building XML and its Base64-compressed export code.
- Reason: both formats can be parsed locally and safely without resolving a third-party share URL.
- Alternative: share URLs. Deferred until a controlled resolver is added.
