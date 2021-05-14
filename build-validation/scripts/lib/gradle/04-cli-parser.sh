#!/usr/bin/env bash
# shellcheck disable=SC2034  # It is common for variables in this auto-generated file to go unused
# Created by argbash-init v2.10.0
# ARG_HELP([This function is overridden later on.])
# ARG_VERSION([print_version],[v],[version],[])
# ARG_OPTIONAL_BOOLEAN([interactive],[i],[],[off])
# ARG_OPTIONAL_SINGLE([first-build],[1],[])
# ARG_OPTIONAL_SINGLE([second-build],[2],[])
# ARG_OPTIONAL_BOOLEAN([debug],[],[],[off])
# ARG_OPTIONAL_SINGLE([mapping-file],[m],[])
# ARGBASH_SET_INDENT([  ])
# ARGBASH_PREPARE()
# needed because of Argbash --> m4_ignore([
### START OF CODE GENERATED BY Argbash v2.10.0 one line above ###
# Argbash is a bash code generator used to get arguments parsing right.
# Argbash is FREE SOFTWARE, see https://argbash.io for more info


die()
{
  local _ret="${2:-1}"
  test "${_PRINT_HELP:-no}" = yes && print_help >&2
  echo "$1" >&2
  exit "${_ret}"
}


begins_with_short_option()
{
  local first_option all_short_options='hvi12m'
  first_option="${1:0:1}"
  test "$all_short_options" = "${all_short_options/$first_option/}" && return 1 || return 0
}

# THE DEFAULTS INITIALIZATION - OPTIONALS
_arg_interactive="off"
_arg_first_build=
_arg_second_build=
_arg_debug="off"
_arg_mapping_file=


print_help()
{
  printf '%s\n' "This function is overridden later on."
  printf 'Usage: %s [-h|--help] [-v|--version] [-i|--(no-)interactive] [-1|--first-build <arg>] [-2|--second-build <arg>] [--(no-)debug] [-m|--mapping-file <arg>]\n' "$0"
  printf '\t%s\n' "-h, --help: Prints help"
  printf '\t%s\n' "-v, --version: Prints version"
}


parse_commandline()
{
  while test $# -gt 0
  do
    _key="$1"
    case "$_key" in
      -h|--help)
        print_help
        exit 0
        ;;
      -h*)
        print_help
        exit 0
        ;;
      -v|--version)
        print_version
        exit 0
        ;;
      -v*)
        print_version
        exit 0
        ;;
      -i|--no-interactive|--interactive)
        _arg_interactive="on"
        test "${1:0:5}" = "--no-" && _arg_interactive="off"
        ;;
      -i*)
        _arg_interactive="on"
        _next="${_key##-i}"
        if test -n "$_next" -a "$_next" != "$_key"
        then
          { begins_with_short_option "$_next" && shift && set -- "-i" "-${_next}" "$@"; } || die "The short option '$_key' can't be decomposed to ${_key:0:2} and -${_key:2}, because ${_key:0:2} doesn't accept value and '-${_key:2:1}' doesn't correspond to a short option."
        fi
        ;;
      -1|--first-build)
        test $# -lt 2 && die "Missing value for the optional argument '$_key'." 1
        _arg_first_build="$2"
        shift
        ;;
      --first-build=*)
        _arg_first_build="${_key##--first-build=}"
        ;;
      -1*)
        _arg_first_build="${_key##-1}"
        ;;
      -2|--second-build)
        test $# -lt 2 && die "Missing value for the optional argument '$_key'." 1
        _arg_second_build="$2"
        shift
        ;;
      --second-build=*)
        _arg_second_build="${_key##--second-build=}"
        ;;
      -2*)
        _arg_second_build="${_key##-2}"
        ;;
      --no-debug|--debug)
        _arg_debug="on"
        test "${1:0:5}" = "--no-" && _arg_debug="off"
        ;;
      -m|--mapping-file)
        test $# -lt 2 && die "Missing value for the optional argument '$_key'." 1
        _arg_mapping_file="$2"
        shift
        ;;
      --mapping-file=*)
        _arg_mapping_file="${_key##--mapping-file=}"
        ;;
      -m*)
        _arg_mapping_file="${_key##-m}"
        ;;
      *)
        _PRINT_HELP=yes die "FATAL ERROR: Got an unexpected argument '$1'" 1
        ;;
    esac
    shift
  done
}


# OTHER STUFF GENERATED BY Argbash

### END OF CODE GENERATED BY Argbash (sortof) ### ])
# [ <-- needed because of Argbash
function print_help() {
  echo "Assists in validating that a Gradle build is optimized for using the remote build cache (Continuous Integration to Local)."
  print_bl
  print_script_usage
  print_option_usage -i
  print_option_usage "-1, --first-build" "Specifies the URL for the build scan of the first build."
  print_option_usage "-2, --second-build" "Specifies the URL for the build scan of the second build."
  print_option_usage -m
  print_option_usage -v
  print_option_usage -h
}
# ] <-- needed because of Argbash