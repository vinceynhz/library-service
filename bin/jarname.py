import os
import sys

from lib import read_app_props

FILE_PATH = os.path.dirname(os.path.realpath(__file__)) + "/.."
POM_PATH = FILE_PATH + "/pom.xml"


def main(argv):
    if 0 == len(argv):
        print(read_app_props(POM_PATH), end='')
    else:
        print(read_app_props(POM_PATH, argv[0]), end='')


if __name__ == '__main__':
    main(sys.argv[1:])
    sys.exit(0)
