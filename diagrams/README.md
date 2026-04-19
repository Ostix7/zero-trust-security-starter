# Діаграми

Каталог містить векторні схеми, які ілюструють архітектуру, розгортання та
ключові сценарії роботи Zero Trust Spring Boot стартера. Ці ж схеми
використовуються як додатки Б–М у тексті кваліфікаційної роботи.

## Структура

- `plantuml/` — вихідні PlantUML файли (редаговані)
- `svg/` — векторні зображення, згенеровані з PlantUML
- `png/` — растрові копії, що вбудовуються у `.docx`

## Перелік діаграм

| Файл | Тип | Опис |
|------|------|------|
| `architecture` | компонентна | Модульна структура стартера та залежності |
| `deployment` | розгортання | Docker Compose демонстраційний ландшафт |
| `filter_chain` | діяльності | Порядок фільтрів у Spring Security |
| `token_propagation` | послідовність | Пробросування JWT через Gateway |
| `service_token` | послідовність | Використання сервісного токена з Vault |
| `policy_evaluation` | послідовність | Делегування рішення зовнішньому policy engine |
| `event_flow` | компонентна | Event-орієнтована спостережуваність |
| `rate_limit` | діяльності | Алгоритм token bucket для rate limiting |
| `usecase` | варіантів використання | Зовнішні дійові особи та сценарії |
| `class_model` | класів | Ключові класи і SPI стартера |

## Перегенерація

```bash
# SVG
java -jar plantuml.jar -charset UTF-8 -tsvg -o ../svg plantuml/*.puml

# PNG для вбудовування у .docx
java -jar plantuml.jar -charset UTF-8 -tpng -o ../png plantuml/*.puml
```
