#!/usr/bin/env bash
#
# Runs Experiment 02 - Validate Build Caching - Local - In Place 
#
# Invoke this script with --help to get a description of the command line arguments
#
SCRIPT_NAME=$(basename "$0")
SCRIPT_DIR="$(cd "$(dirname "$(readlink -e "${BASH_SOURCE[0]}")")" && pwd)"
LIB_DIR="${SCRIPT_DIR}/../lib"

# Experiment-speicifc constants
EXP_NAME="Validate Build Caching - Local - In Place"
EXP_NO="02"
EXP_SCAN_TAG=exp2-gradle
EXPERIMENT_DIR="${SCRIPT_DIR}/data/${SCRIPT_NAME%.*}"
SCAN_FILE="${EXPERIMENT_DIR}/scans.csv"

build_cache_dir="${EXPERIMENT_DIR}/build-cache"

# These will be set by the config functions (see lib/config.sh)
project_url=''
project_name=''
project_branch=''
tasks=''
extra_args=''
enable_ge=''
ge_server=''

# Include and parse the command line arguments
# shellcheck source=experiments/lib/gradle/02/parsing.sh
source "${LIB_DIR}/gradle/02/parsing.sh" || { echo "Couldn't find '${LIB_DIR}/gradle/02/parsing.sh' parsing library."; exit 1; }
# shellcheck source=experiments/lib/libs.sh
source "${LIB_DIR}/libs.sh" || { echo "Couldn't find '${LIB_DIR}/libs.sh'"; exit 1; }

RUN_ID=$(generate_run_id)

main() {
  if [ "$_arg_interactive" == "on" ]; then
    wizard_execute
  else
    execute
  fi
}

execute() {
  load_config
  validate_required_config

  make_experiment_dir
  make_local_cache_dir

  clone_project ""
  execute_first_build
  execute_second_build

  print_warnings
  print_summary
}

wizard_execute() {
  print_introduction

  make_experiment_dir

  load_config
  collect_project_details

  explain_collect_gradle_task
  collect_gradle_task

  explain_clone_project
  clone_project ""

  explain_local_cache_dir
  make_local_cache_dir

  explain_scan_tags
  explain_first_build
  execute_first_build

  explain_second_build
  execute_second_build

  save_config

  print_warnings
  explain_warnings

  print_summary
  explain_summary
}

execute_first_build() {
  info "Running first build:"
  execute_build
}

execute_second_build() {
  info "Running second build:"
  execute_build
}

execute_build() {
  # The gradle --init-script flag only accepts a relative directory path. ¯\_(ツ)_/¯
  local lib_dir_rel
  lib_dir_rel=$(realpath --relative-to="$( pwd )" "${LIB_DIR}")

  info "./gradlew -Dscan.tag.${EXP_SCAN_TAG} -Dscan.tag.${RUN_ID} clean ${tasks}$(print_extra_args)"

  invoke_gradle \
     --init-script "${lib_dir_rel}/gradle/verify-and-configure-local-build-cache-only.gradle" \
     clean "${tasks}"
}

print_summary() {
 read_scan_info

 local branch
 branch=$(git symbolic-ref --short HEAD)

 local fmt="%-25s%-10s"
 info "Summary"
 info "-------"
 infof "$fmt" "Project:" "${project_name}"
 infof "$fmt" "Git repo:" "${project_url}"
 infof "$fmt" "Git branch:" "${branch}"
 infof "$fmt" "Gradle tasks:" "${tasks}"
 infof "$fmt" "Gradle arguments:" "${_arg_args}"
 infof "$fmt" "Experiment:" "${EXP_NO}-${EXP_NAME}"
 infof "$fmt" "Experiment id:" "${EXP_SCAN_TAG}"
 infof "$fmt" "Experiment run id:" "${RUN_ID}"
 infof "$fmt" "Experiment artifact dir:" "${EXPERIMENT_DIR}"
 print_build_scans
 print_quick_links
}

print_build_scans() {
 local fmt="%-25s%-10s"
 infof "$fmt" "Build scan first build:" "${scan_url[0]}"
 infof "$fmt" "Build scan second build:" "${scan_url[1]}"
}

print_quick_links() {
 local fmt="%-25s%-10s"
 info 
 info "Investigation quick links"
 info "-------------------------"
 infof "$fmt" "Build scan comparison:" "${base_url[0]}/c/${scan_id[0]}/${scan_id[1]}/task-inputs?cacheability=cacheable"
 infof "$fmt" "Task execution summary:" "${base_url[0]}/s/${scan_id[1]}/performance/execution"
 infof "$fmt" "Cache performance:" "${base_url[0]}/s/${scan_id[1]}/performance/build-cache"
 infof "$fmt" "Executed tasks:" "${base_url[0]}/s/${scan_id[1]}/timeline?outcome=SUCCESS,FAILED&sort=longest"
 infof "$fmt" "Executed cachable tasks:" "${base_url[0]}/s/${scan_id[1]}/timeline?cacheableFilter=cacheable&outcomeFilter=SUCCESS,FAILED&sorted=longest"
 infof "$fmt" "Uncachable tasks:" "${base_url[0]}/s/${scan_id[1]}/timeline?cacheableFilter=any_non-cacheable&outcomeFilter=SUCCESS,FAILED&sorted=longest"
 info
}

print_introduction() {
  local text
  IFS='' read -r -d '' text <<EOF
$(print_introduction_title)

This is the second of several experiments designed to help you
optimize your team's builds. If you are running this experiment as part of a
Gradle Enterprise Trial, then the experiments will also help you to build
the data necessary to determine if Gradle Enerprise is useful to your
organization.

This script (and the other experiment scripts) will run some of the
experiment steps for you, but we'll walk you through each step so that you
know exactly what we are doing, and why.

In this experiment, we will be checking your build to see how well it takes
advantage of the local build cache. When the build cache is enabled, Gradle
saves the output from tasks so that the same output can be reused if the
task is executed again with the same inputs. This is similar to incremental
build, except that the cache is used across build runs. So even if you
perform a clean, cached output will be used if the inputs to a task have not
changed.

To test out the build cache, we'll run two builds (with build caching
enabled). Both builds will invoke clean and run the same tasks. We will not
make any changes between each build run.

If the build is taking advantage of the local build cache, then very few (if
any) tasks should actually execute on the seond build (all of the task
output should be used from the local cache).

The Gradle Solutions engineer will then work with you to figure out why some
(if any) tasks ran on the second build, and how to optimize them to take
advantage of the build cache.

${USER_ACTION_COLOR}Press enter when you're ready to get started.
EOF

  print_in_box "${text}"
  wait_for_enter
}

explain_local_cache_dir() {
  local text
  IFS='' read -r -d '' text <<EOF
We are going to create a new empty local build cache dir (and configure
Gradle to use it instead of the default local cache dir). This way, the
first build won't find anything in the cache and all tasks will run. 

This is mportant beause we want to make sure tasks that are cachable do in
fact produce output that is stored in the cache.

Specifically, we are going to create and use this directory for the local
build cache (we'll delete it if it already exists from a previous run of the
experiment):

$(info "${build_cache_dir}")

${USER_ACTION_COLOR}Press enter to continue.
EOF
  print_in_box "${text}"
  wait_for_enter
}

explain_first_build() {
 local build_command
  build_command="${INFO_COLOR}./gradlew \\
  ${INFO_COLOR}-Dscan.tag.${EXP_SCAN_TAG} \\
  ${INFO_COLOR}-Dscan.tag.${RUN_ID} \\
  ${INFO_COLOR} clean ${tasks}"

  local text
  IFS='' read -r -d '' text <<EOF
OK! We are ready to run our first build!

For this run, we'll execute 'clean ${tasks}'. 

We are invoking clean even though we just created a fresh clone because
sometimes the clean task changes the order other tasks run in, which can
impact how the build cache is utilized.

We will also add the build scan tags we talked about before.

Effectively, this is what we are going to run:

${build_command}

${USER_ACTION_COLOR}Press enter to run the first build.
EOF
  print_in_box "${text}"
  wait_for_enter
}

explain_second_build() {
  local text
  IFS='' read -r -d '' text <<EOF
Now we are going to run the build again without changing anything.

In a fully optimized build, no tasks would run on this second build because
we already built everything in the first build, and the task outputs should
be in the local build cache. If some tasks do run, they will show up in the
build scan for this second build.

${USER_ACTION_COLOR}Press enter to run the second build.
EOF
  print_in_box "$text"
  wait_for_enter
}

explain_summary() {
  read_scan_info
  local text
  IFS='' read -r -d '' text <<EOF
Now that both builds have completed, there is a lot of valuable data in
Gradle Enterprise to look at. The data can help you find ineffiencies in
your build.

After running the experiment, this script will generate a summary table of
useful data and links to help you analyze the experiment results. Most of
the data in the summmary is self-explanatory, but a few are worth reviewing:

$(print_build_scans)

^^ These are links to the build scans for the builds. A build scan provides
a wealth of information and statistics about the build execution.

$(print_quick_links)

^^ These are links to help you get started in your analysis. The first link
is to a comparison of the two build scans. comparisons show you what was
different between two different build executions.

The "Cache performance" link takes you to the build cache performance page
of the 2nd build scan. This page contains various metrics related to the
build cache (such as cache hits and misses).

The "Executed cachable tasks" link shows you which tasks ran again on the
second build, but shouldn't have because they are actually cachable. If any
cachable tasks ran, then one of their inputs changed (even though we didn't
make any changes), or they may not be declaring their inputs correctly.

The last link, "Uncachable tasks", shows you which tasks ran that are not
cachable. It is not always possible (or doesn't make sense) to cache the
output from every task. For example, there is no way to cache the "output"
of the clean task because the clean task deletes output rather than creating
it.

If you find some optimizations, then it is recommended to run this expirment
again (to validate the optimizations were effective). You do not need to run in
interactive mode again. All of your settings have been saved so that you can
repeate the experiment by specifying the configuration when invoking the
script:

$(info "./${SCRIPT_NAME} -c ${SCRIPT_NAME%.*}.config")

Congrats! You have completed this experiment.
EOF
  print_in_box "${text}"
}

parse_commandline "$@"
main
