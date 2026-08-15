MERGE INTO mechanics (name, game_version, title, explanation, source_url, collected_at, reviewed)
KEY (name, game_version)
VALUES ('Righteous Fire', '3.29', 'Righteous Fire의 생명력 기반 화염 지속 피해',
        'Righteous Fire는 최대 생명력과 에너지 보호막을 기반으로 주변 적에게 화염 지속 피해를 주며, 자신에게도 화염 지속 피해를 적용합니다.',
        'https://www.pathofexile.com/', DATE '2026-08-15', TRUE);
