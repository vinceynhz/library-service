#!/usr/bin/python3
import argparse
import os
import sys

from lib import run_mvn_command

FILE_PATH = os.path.dirname(os.path.realpath(__file__)) + "/.."


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("-g", "--goals", nargs="*", help="Maven goals to run")
    parser.add_argument("-o", "--options", nargs="*", help="Maven options to run")

    args = parser.parse_args()

    return run_mvn_command(FILE_PATH, args.goals, args.options)


if __name__ == '__main__':
    rc = main()
    sys.exit(rc)
