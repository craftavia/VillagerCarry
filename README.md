# VillagerCarry

**Pick up a villager into an egg and place it somewhere else, with its profession, trades and discounts intact.**

Moving villagers to a trading hall usually means boats, minecarts and a lot of patience. With VillagerCarry a
player sneaks, right-clicks the villager with an empty hand and gets a villager egg. Right-clicking a block
with the egg puts the same villager back: same profession and level, same trades and uses, same reputation
(so cured-villager discounts stay), same name.

## Features

- **Sneak + right-click** with an empty main hand to pick a villager up; **right-click a block** to place it.
- **Everything that matters is kept**: profession, level and XP, all trades and their uses, gossip/reputation
  (discounts), biome type, custom name, health, baby/adult.
- **Readable egg** – the egg's description lists type, health, profession, XP bar, its previous bed and
  workstation and every trade with enchantments and remaining uses.
- **Workstation and bed are released** when a villager is picked up, so another villager can take them.
  At the new place the villager looks for a new bed and workstation.
- **Duplication-safe** – every egg has a unique ID and works once; a copied egg is rejected and removed.
  Eggs do not stack, cannot be used on mobs or spawners and cannot be fired from dispensers.
- **Not while trading or leashed**, optional baby pick-up, per-world allow list, cooldown.
- **WorldGuard support** (optional) – players can only pick up and place villagers where they are allowed to
  build. Other protection plugins that cancel the interaction are respected as well.
- Per-player language (English and Turkish bundled), console log of every move, no database,
  no network calls, no metrics.

## Requirements

| | |
|---|---|
| Server | Paper or a fork such as Purpur, **1.21.4 – 1.21.x and 26.x** |
| Java | 21 or newer |
| Optional | WorldGuard 7 |

Folia is not supported.

## Installation

1. Download `VillagerCarry-x.y.z.jar` from [Releases](https://github.com/craftavia/VillagerCarry/releases).
2. Put it in your `plugins/` folder and start the server.
3. Optionally adjust `plugins/VillagerCarry/config.yml` and run `/villagercarry reload`.

## Commands and permissions

| Command / action | Description | Permission | Default |
|---|---|---|---|
| Sneak + right-click a villager | Pick it up into an egg | `villagercarry.use` | everyone |
| Right-click a block with the egg | Place the villager | `villagercarry.use` | everyone |
| — | Ignore the WorldGuard check | `villagercarry.admin` | op |
| `/villagercarry reload` (`/vcarry`) | Reloads config and messages | `villagercarry.admin` | op |

## Configuration

```yaml
worlds: []             # empty = all worlds; "world", "minecraft:overworld" or "overworld"
cooldown-ms: 1000
allow-babies: true
worldguard-check: true
log: true              # "<player> picked up a villager: ..." in the console
default-language: en
```

Messages, the egg name and its description are in `messages_en.properties` and `messages_tr.properties`
(MiniMessage). Players see the file matching their client language; the egg description is written in the
language of the player who picks the villager up. Add a language by dropping `messages_<code>.properties` in
the plugin folder.

### Notes

- The villager is stored in the egg as entity data (with temporary fields such as position and brain
  memories removed to keep eggs small). Place carried villagers before you upgrade the server to a new
  Minecraft version.
- Used egg IDs are kept in `plugins/VillagerCarry/used-eggs.txt`. Do not delete it, or old copies of used
  eggs would work again.
- Releasing the workstation uses an internal server method (`releaseAllPois`). If a future server version
  renames it, the plugin keeps working and logs one warning; only the old workstation stays claimed until
  the villager it belonged to is gone.
- Started on Paper 1.21.4 and 26.2 (with and without WorldGuard) before release. The plugin it comes
  from runs on Craftavia's survival server (Purpur 26.2).

## Building

```bash
./gradlew build
```

The jar is written to `build/libs/`.

Made by the Craftavia team, used on play.craftavia.net, https://craftavia.net

## Türkçe

VillagerCarry, köylüyü **eğilip boş elle sağ tıklayarak yumurtaya almanızı** ve istediğiniz yere
koymanızı sağlayan ücretsiz bir Paper eklentisidir. Meslek, seviye, takaslar, kullanım sayıları, itibar
(indirimler), biyom türü ve isim korunur. Köylü alınınca iş bloğu ve yatağı boşa çıkar, yeni yerinde
kendine yenisini bulur.

- Her yumurtanın tek bir kimliği vardır, bir kez çalışır; kopyalanan yumurta silinir.
- Takas yapılan ya da tasmalı köylü alınamaz. Yumurta spawner'a, canlıya ve dağıtıcıya kullanılamaz.
- WorldGuard kuruluysa oyuncu yalnız inşa izni olan yerde köylü alıp koyabilir.
- Oyun dili Türkçe olan oyuncu mesajları ve yumurta açıklamasını Türkçe görür.

Kurulum: jar'ı `plugins/` klasörüne atın ve sunucuyu açın. Ayarlar `plugins/VillagerCarry/config.yml`
dosyasındadır, değişiklikten sonra `/villagercarry reload` yazın.

Craftavia ekibi tarafından geliştirildi, play.craftavia.net sunucusunda kullanılıyor: https://craftavia.net

## License

[MIT](LICENSE) © Craftavia
