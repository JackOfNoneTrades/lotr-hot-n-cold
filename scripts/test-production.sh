#!/usr/bin/env bash
set -euo pipefail

# Uses installed, unmodified release jars and launcher libraries. No account or access token is read.
repo=$(cd "$(dirname "$0")/.." && pwd)
launcher=${FJORD_ROOT:-"$HOME/.local/share/FjordLauncher"}
instance=${FJORD_INSTANCE:-"$launcher/instances/lotr-test"}
side=${1:-client}
profile=${2:-friend}
case "$side" in client|server) ;; *) exit 2 ;; esac
case "$profile" in full|friend|terrain|baseline|compat|streams|worldgen|combined|sources|null-control|null-block|null-biome|null-wotr|null-add) ;; *) exit 2 ;; esac
if [[ "$side" == server && "$profile" == baseline ]]; then
    printf 'Use client baseline: unpatched WOTR refuses a dedicated server\n' >&2
    exit 2
fi
mkdir -p "$repo/run"
run_dir=${PRODUCTION_TEST_DIR:-$(mktemp -d "$repo/run/production-$side-$profile-XXXXXX")}
[[ "$run_dir" == /* ]] || { printf 'PRODUCTION_TEST_DIR must be absolute\n' >&2; exit 2; }
mkdir -p "$run_dir/mods" "$run_dir/config"
if [[ "$profile" == baseline && -f "$run_dir/mods/hotncold.jar" ]]; then
    printf 'Baseline must use a directory without Hot N Cold installed\n' >&2
    exit 2
fi
if [[ ${WITH_BADMOBS:-false} != true && -f "$run_dir/mods/BadMobs-1.0.1-1.7.10.jar" ]]; then
    printf 'This reused directory has Bad Mobs; use WITH_BADMOBS=true or a new directory\n' >&2
    exit 2
fi
printf 'Production test directory: %s\n' "$run_dir"
if [[ -f "$run_dir/console.log" ]]; then
    archive=$(mktemp -d "$run_dir/previous-XXXXXX")
    for result in console.log production-result.txt terrain-snapshot.txt streams-result.txt artifacts.sha256 screenshots; do
        if [[ -e "$run_dir/$result" ]]; then
            mv "$run_dir/$result" "$archive/$result"
        fi
    done
fi
trap 'status=$?; if (( status != 0 )); then printf "Production test failed; full log: %s/console.log\n" "$run_dir" >&2; tail -n 45 "$run_dir/console.log" >&2; fi' EXIT

if [[ -n ${PRODUCTION_BASE_MODS:-} ]]; then
    while IFS= read -r -d '' mod; do
        cp "$mod" "$run_dir/mods/"
    done < <(find "$PRODUCTION_BASE_MODS" -maxdepth 1 -type f \( -name '*.jar' -o -name '*.zip' \) -print0)
else
    for mod in '+unimixins-all-1.7.10-0.2.1.jar' 'LOTRMod v36.14.jar' 'War of the Ring-1.3.1.jar' 'DrZharks MoCreatures Mod v6.3.1.zip' 'IvToolkit-1.2.1.jar' 'EnviroMine-1.3.148-ESE-0x01.jar'; do
        cp "$instance/.minecraft/mods/$mod" "$run_dir/mods/$mod"
    done
fi
extra_java=()
if [[ ${REQUIRE_HISTORY_ITEMS:-false} == true ]]; then
    [[ "$profile" == friend || "$profile" == combined || "$profile" == sources ]] || { printf 'History Items requires friend, combined or sources profile\n' >&2; exit 2; }
    extra_java+=(-Dhotncold.fixture.requireHistoryItems=true)
fi
if [[ ${WITH_BADMOBS:-false} == true ]]; then
    cp "$instance/.minecraft/mods/BadMobs-1.0.1-1.7.10.jar" "$run_dir/mods/BadMobs-1.0.1-1.7.10.jar"
    extra_java+=(-Dhotncold.fixture.expectBadMobs=true)
fi
if [[ "$profile" != baseline ]]; then
    artifact=${PRODUCTION_MOD_JAR:-$(find "$repo/build/libs" -maxdepth 1 -type f -name 'hotncold-*.jar' ! -name '*-dev.jar' ! -name '*-sources.jar' -printf '%T@ %p\n' | sort -nr | head -n 1 | cut -d' ' -f2-)}
    cp "$artifact" "$run_dir/mods/hotncold.jar"
fi
if [[ -n ${PRODUCTION_EXTRA_MODS:-} ]]; then
    while IFS= read -r -d '' mod; do
        cp "$mod" "$run_dir/mods/"
    done < <(find "$PRODUCTION_EXTRA_MODS" -maxdepth 1 -type f -name '*.jar' -print0)
fi
cp "$repo/build/production-test/hotncold-production-fixture.jar" "$run_dir/mods/hotncold-production-fixture.jar"
if [[ ( "$profile" == friend || "$profile" == combined ) && ! -f "$run_dir/config/hotncold.cfg" ]]; then
    cp "${FRIEND_CONFIG:-/tmp/hotncold.cfg}" "$run_dir/config/hotncold.cfg"
fi
if [[ -n ${PRODUCTION_CONFIG:-} && ! -f "$run_dir/config/hotncold.cfg" ]]; then
    cp "$PRODUCTION_CONFIG" "$run_dir/config/hotncold.cfg"
fi
(cd "$run_dir" && find mods -maxdepth 1 -type f -print0 | sort -z | xargs -0 sha256sum > artifacts.sha256)

classpath=''
while IFS=: read -r group artifact version classifier; do
    relative="${group//.//}/$artifact/$version/$artifact-$version${classifier:+-$classifier}.jar"
    lib="$launcher/libraries/$relative"
    [[ -f "$lib" ]] || { printf 'Missing runtime library: %s\n' "$lib" >&2; exit 1; }
    classpath+="${classpath:+:}$lib"
done < <(jq -sr '[.[].libraries[] | select(.natives == null)] | reduce .[] as $l ({}; .[($l.name | split(":")[0:2] | join(":"))] = $l.name) | .[]' \
    "$launcher/meta/org.lwjgl/2.9.4-nightly-20150209.json" \
    "$launcher/meta/net.minecraft/1.7.10.json" \
    "$launcher/meta/net.minecraftforge/10.13.4.1614.json")

java=${PRODUCTION_JAVA:-/usr/lib/jvm/java-8-openjdk/bin/java}
common=(-Xms512m -Xmx4096m "-Dhotncold.fixture.profile=$profile" -Dhotncold.fixture.spawnStressIterations=100000 "${extra_java[@]}")
cd "$run_dir"
if [[ "$side" == client ]]; then
    classpath+=":$launcher/libraries/com/mojang/minecraft/1.7.10/minecraft-1.7.10-client.jar"
    timeout 300s xvfb-run -a "$java" "${common[@]}" "-Djava.library.path=$instance/natives" -cp "$classpath" net.minecraft.launchwrapper.Launch \
        --tweakClass cpw.mods.fml.common.launcher.FMLTweaker --username ProductionTest --accessToken 0 --version 1.7.10 \
        --gameDir "$run_dir" --assetsDir "$launcher/assets" --assetIndex 1.7.10 --userProperties '{}' > console.log 2>&1
else
    server_jar=${MINECRAFT_SERVER_JAR:-"${GRADLE_USER_HOME:-$HOME/.gradle}/caches/minecraft/net/minecraft/minecraft_server/1.7.10/minecraft_server-1.7.10.jar"}
    [[ -f "$server_jar" ]] || { printf 'Set MINECRAFT_SERVER_JAR to the original 1.7.10 server jar\n' >&2; exit 1; }
    cp "$repo/scripts/production-server.properties" server.properties
    cp "$repo/run/server/eula.txt" eula.txt
    classpath+=":$server_jar"
    timeout 300s "$java" "${common[@]}" -cp "$classpath" net.minecraft.launchwrapper.Launch \
        --tweakClass cpw.mods.fml.common.launcher.FMLServerTweaker nogui > console.log 2>&1
fi
test -f production-result.txt
rg 'PRODUCTION_|FRIEND_|SERVER_FIXTURE_PASSED|SERVER_SPAWN_STRESS_PASSED' console.log
printf 'Artifacts and full log: %s\n' "$run_dir"
