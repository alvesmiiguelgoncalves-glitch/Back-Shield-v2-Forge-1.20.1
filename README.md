# Back Shield (Forge 1.20.1)

Port do mod Fabric 1.21.1 para **Minecraft Forge 1.20.1**. O **Trinkets** foi trocado pelo **Curios API**.

- Slot **"Escudo nas costas"** (`back_shield`) na tela do Curios. Só aceita escudos (`ShieldItem` ou qualquer item com a ação `SHIELD_BLOCK`, inclusive de outros mods).
- **G** (remapeável): saca o escudo das costas para a mão secundária / guarda de volta. Com o escudo na mão principal ou secundária e o slot vazio, G guarda o escudo nas costas.
- **R** (segurar, remapeável): defesa temporária. O escudo vai para a mão secundária, o bloqueio é ativado sozinho e, ao soltar, ele volta para as costas.
- Posição calculada a partir do torso do modelo (agachar, nadar, animações), com perfis para peitoral, elytra, voo e mochila.

## Dependências

| Mod | Obrigatório |
|---|---|
| Forge 47.x (1.20.1) | sim |
| Curios API >= 5.4.0 | sim |
| Backpacked | não (ver limitações) |

Servidor e cliente precisam ter o mod e o Curios.

## Configuração visual (cliente)

Igual à versão Fabric: `config/backshield-client.json`, comandos `/backshield status | show | set | reset | reload`
e 16 perfis independentes (veja o README original). A tela de config abre pelo botão **Config** na lista de mods do Forge.

## Compilar

```
./gradlew build
```
O jar sai em `build/libs/backshield-1.2.0.jar`. Requer JDK 17. O Gradle baixa Forge e Curios na primeira vez.

## O que mudou em relação ao Fabric

| Fabric 1.21.1 | Forge 1.20.1 |
|---|---|
| Trinkets (`data/trinkets/slots/chest/back_shield.json`) | Curios (`data/backshield/curios/slots/back_shield.json` + `curios/entities`) |
| `TrinketRenderer` | `ICurioRenderer` + `CuriosRendererRegistry` |
| Fabric networking (payload) | `SimpleChannel` |
| Mod Menu | `ConfigScreenFactory` do Forge |
| Yarn | Mappings oficiais da Mojang |
| Java 21 | Java 17 |

## Limitações / não testado

- **Este port nunca foi compilado nem rodado no jogo** (foi escrito sem acesso a rede). Pode haver pequenos ajustes de assinatura, principalmente na API do Curios; se o Curios `5.11.0+1.20.1` não resolver, ajuste `curios_version` em `gradle.properties`.
- Os ângulos/posições do escudo foram mantidos; confirme visualmente no jogo.
- **BackSlot** é exclusivo do Fabric: os perfis `item1`/`item2` nunca são escolhidos automaticamente no Forge.
- **Backpacked**: a detecção por reflexão usa nomes do Backpacked 3.x (1.21.1). Na versão de 1.20.1 ela provavelmente falha com segurança (o perfil `backpack` fica sem uso) e precisa ser adaptada.
