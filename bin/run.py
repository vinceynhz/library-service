#!/usr/bin/python3
import argparse
import os
import sys

from lib import run_jar_command, read_app_props

FILE_PATH = os.path.dirname(os.path.realpath(__file__)) + "/.."
POM_PATH = FILE_PATH + "/pom.xml"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("-j", "--jvm-opts", nargs="*", help="JVM opts")
    parser.add_argument("-a", "--app-opts", nargs="*", help="App opts")
    args = parser.parse_args()

    jar_name = "./target/" + read_app_props(POM_PATH)

    return run_jar_command(jar_name, args.jvm_opts, args.app_opts)


if __name__ == '__main__':
    rc = main()
    sys.exit(rc)
