# parity

What sodium, lithium and ferritecore do on 1.21.8, and where quillcore stands.
Clean-room: built from each project's public docs, wikis, config lists and changelogs.
Nothing here is endorsed by those projects.

have: `yes` on main, `pr` open pr, `part` partial/different approach, `no`.
impact / effort: 1 (low) to 3 (high).

## render (sodium)

| area | feature | have | impact | effort |
|---|---|---|---|---|
| meshing | off-thread section meshing with priority queue | no (vanilla) | 3 | 3 |
| meshing | compact terrain vertex format | part (solid layer, m1) | 3 | 2 |
| meshing | faster block/fluid model emit, no per-quad allocs | no | 2 | 2 |
| meshing | biome blend colour cache per section | no | 2 | 1 |
| batching | per-region buffer arenas | part (allocator, not wired) | 3 | 3 |
| batching | multidraw per region/pass | part (batch lists, not wired; needs region-relative positions, see below) | 3 | 2 |
| batching | per-face index split (draw only facing sides) | no | 2 | 2 |
| culling | section graph occlusion culling (bfs) | n/a, vanilla has SectionOcclusionGraph; a faster one is the work | 2 | 3 |
| culling | tighter frustum test per region | no | 2 | 1 |
| culling | block face culling cache | no | 1 | 2 |
| translucency | per-section translucent sorting off-thread | no | 2 | 3 |
| lighting | smooth lighting / ao rewrite, fewer artifacts | no | 1 | 2 |
| lighting | flat light cache per mesh build | no | 1 | 1 |
| particles | particle frustum culling | part (distance cutoff) | 2 | 1 |
| particles | batched particle buffer writes | no | 1 | 2 |
| entities | entity frustum + distance culling | part (distance cutoff) | 2 | 1 |
| entities | block entity culling | part (distance cutoff) | 2 | 1 |
| entities | fast model part / cuboid emit | no | 2 | 2 |
| textures | skip animating off-screen sprites | no | 2 | 1 |
| textures | mipmap edge fixes | no | 1 | 1 |
| gui | video settings screen with more knobs | part (config screen) | 1 | 2 |
| gui | cpu render-ahead limit | no | 1 | 1 |
| misc | clouds/weather radius caps | yes | 1 | 1 |
| misc | upscaling (fsr1) — not in sodium | yes | - | - |

## logic (lithium)

| area | feature | have | impact | effort |
|---|---|---|---|---|
| ai | secondary_poi: skip scan for jobs with no secondary poi | pr | 1 | 1 |
| ai | sensor/task stream replacement | no | 1 | 1 |
| ai | useless sensors (baby, goat) | no | 1 | 1 |
| ai | nitwit job search skip | pr #23 | 1 | 1 |
| ai | task memory change tracking / run loop | no | 2 | 2 |
| ai | idle mob ai / brain throttle — not vanilla-exact | yes | - | - |
| pathing | faster path node type lookups | no | 2 | 2 |
| pathing | inactive navigations skipped | part (idle throttle) | 2 | 1 |
| poi | poi lookups by type / fast portals | no | 2 | 2 |
| poi | raid / non-poi block search | no | 1 | 1 |
| collisions | movement collision fast path | no | 3 | 3 |
| collisions | intersection checks / unpushable cramming | no | 2 | 2 |
| collisions | block effect collection | no | 1 | 1 |
| entities | entity class groups per section | no | 2 | 2 |
| entities | equipment / enchant tick tracking | no | 1 | 2 |
| entities | elytra / powder snow / sprint particle checks | no | 1 | 1 |
| entities | projectile collisions | no | 1 | 1 |
| entities | item merge — vanilla-exact | part (throttle, not exact) | 2 | 2 |
| block ticks | sleeping block entities (furnace, hopper, campfire, brewing, crafter, chest) | pr (furnace, campfire early-out) | 3 | 2 |
| block ticks | block entity ticker collections | no | 1 | 1 |
| block ticks | fluid flow caching | no | 2 | 2 |
| hoppers | inventory change tracking, no entity scan when idle | no | 3 | 3 |
| redstone | wire power calc without repeated lookups | no | 2 | 2 |
| redstone | enum_values: no Direction.values() clones | pr | 1 | 1 |
| shapes | blockstate shape cache / precomputed arrays / specialized shapes | no | 2 | 2 |
| shapes | shape merging | no | 1 | 1 |
| chunk | no_locking on paletted containers | pr | 2 | 1 |
| chunk | palette fast path | no | 1 | 2 |
| chunk | serialization / no_validation | no | 1 | 1 |
| chunk | chunk ticket collections | no | 1 | 1 |
| world gen | cached generator settings | n/a (1.21.8 reads a holder field; nothing to cache) | 1 | 1 |
| world gen | chunk random alloc | no | 1 | 1 |
| collections | attributes, brain, entity_by_type, mob spawning, poi types | no | 1 | 1 |
| math | fast blockpos / util / sine lut | no | 1 | 1 |
| alloc | composter, nbt, explosion, entity tracker | no | 1 | 1 |
| client tick | unused brain / ambient sound / biome particles | no | 1 | 1 |
| hashcode | cached_hashcode — already identity hash in 1.21.8 | n/a | - | - |

## memory (ferritecore)

| feature | have | impact | effort |
|---|---|---|---|
| shared neighbour tables for block states (replaces per-state map) | pr | 3 | 2 |
| baked quad vertex dedup | pr | 2 | 1 |
| property value condition optionals | n/a (class gone in 1.21.8) | - | - |
| multipart predicate cache | n/a (1.21.8 builds predicates once per block) | - | - |
| blockstate property map: fast map instead of immutable map | n/a (1.21.8 uses a small array map) | - | - |
| model resource location strings | n/a (block models no longer keyed by MRL) | - | - |
| multipart model instances shared | n/a (vanilla shares baked state) | - | - |
| blockstate shape cache dedup | pr | 2 | 1 |
| nbt of partially loaded chunks | no | 1 | 2 |

## counts

| mod | rows | yes | pr | part | no |
|---|---|---|---|---|---|
| sodium | 21 | 1 | 0 | 7 | 13 |
| lithium | 35 | 0 | 5 | 2 | 28 |
| ferritecore | 4 | 0 | 3 | 0 | 1 |

(extras that are ours only, and n/a rows, aren't counted. five ferritecore items target things 1.21.8 already fixed.)

## render notes: why multidraw isn't wired yet

Checked against 1.21.8 Blaze3D:

- `RenderPass.Draw` has no base vertex. `GlCommandEncoder.drawFromBuffers` takes one internally, but the public path doesn't expose it.
- `drawMultipleIndexed` issues one GL draw per entry, and it uploads the `DynamicTransforms` block (ModelOffset) per entry.
- Our compact positions are section-local 16-bit, (p + 8) * 2048, so a draw can't span sections.

Plan:
1. Put the section's slot (0..255) in the spare bits of Position.w, next to the face index.
2. The shader adds the slot offset, and ModelOffset becomes region origin minus camera.
3. Compact SOLID uploads go into the region arena buffer.
4. Each region draws through the sequential quad index buffer, using firstIndex = vertexOffset / 4 * 6 and merging touching ranges.
5. A raw-GL glMultiDrawElements path comes after.

The client has to render correctly at each step, and each step needs a G14 frame-time run.

## roadmap (impact / effort)

1. chunk no_locking — pr
2. ferrite neighbour tables — pr
3. ferrite quad dedup — pr
4. region multidraw on m1 compact meshes — allocator pr; next: region-relative vertices, then gl wiring; needs g14 run
5. sleeping block entities — furnace/campfire pr; brewing, crafter, hopper next
6. biome blend cache — needs g14 run
7. skip off-screen sprite animation — needs g14 run
8. entity / block entity / particle frustum culling — needs g14 run
9. faster section graph (vanilla already culls) — needs g14 run
10. hopper inventory tracking

Then: off-thread meshing, entity class groups, collision fast path, shape caches, translucent sorting.
