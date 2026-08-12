MERGE INTO mechanics (name, game_version, title, explanation, source_url, collected_at, reviewed)
KEY (name, game_version)
VALUES ('Fireball', '3.27', 'Fireball deals fire spell damage',
        'Fireball is a spell that hits enemies with fire damage. Support gems and modifiers that apply to fire, spell, projectile, or area damage can change how the build scales it.',
        'https://www.pathofexile.com/', DATE '2026-08-11', TRUE);

MERGE INTO mechanics (name, game_version, title, explanation, source_url, collected_at, reviewed)
KEY (name, game_version)
VALUES ('Arc', '3.27', 'Arc chains lightning spell damage',
        'Arc is a lightning spell that chains between enemies. Modifiers that apply to lightning, spell, or chain effects can change how the build scales it.',
        'https://www.pathofexile.com/', DATE '2026-08-11', TRUE);
