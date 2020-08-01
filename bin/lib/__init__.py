import contextlib
import os
import signal
import subprocess
from mmap import mmap

default_goals = "clean package"
default_jvm_opts = "-Xms64M -Xmx1024M"

_running_command = None
_signal_captured = False


# noinspection PyUnusedLocal
def _sig_handler(signum, frame):
    global _running_command
    global _signal_captured
    _signal_captured = True
    # We have an app running and it's actually running
    if _running_command is not None and _running_command.poll() is None:
        _running_command.send_signal(signal.SIGTERM)


signal.signal(signal.SIGINT, _sig_handler)
signal.signal(signal.SIGTERM, _sig_handler)


@contextlib.contextmanager
def chd(new_dir):
    prev_dir = os.getcwd()
    os.chdir(new_dir)
    yield
    os.chdir(prev_dir)


def _run_cmd(src_path, command):
    global _running_command
    global _signal_captured
    with chd(src_path):
        print(">", command)
        with subprocess.Popen(command, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE) as cmd:
            _running_command = cmd
            for line in cmd.stdout:
                print(line.decode("utf-8"), end='')

    if not _signal_captured and cmd.returncode != 0:
        raise subprocess.CalledProcessError(cmd.returncode, command)

    return 0


def run_mvn_command(src_path, goals=None, options=None):
    if goals is None:
        goals = default_goals
    else:
        goals = " ".join(goals)

    if options is None:
        opts = ""
    elif len(options) == 1:
        opts = " -D" + options[0]
    else:
        opts = " -D".join(options)

    command = "mvn " + goals + opts

    return _run_cmd(src_path, command)


def run_jar_command(src_path, app_name, jvm=None, jva=None):
    if jvm is None:
        jvm = default_jvm_opts
    else:
        jvm = default_jvm_opts + " " + " ".join(jvm)

    if jva is None:
        jva = ""
    else:
        jva = " " + " ".join(jva)

    command = "java " + jvm + " -jar " + app_name + jva

    return _run_cmd(src_path, command)


def extract_from_tag(mfile, tag):
    mfile.seek(mfile.find(tag) + len(tag) + 1)
    return mfile.readline().decode("utf-8").split("<")[0]


def read_app_props(pom_path, values='jar'):
    with open(pom_path, "r+") as infile:
        mfile = mmap(infile.fileno(), 0)
        app_name = extract_from_tag(mfile, b'artifactId')
        app_ver = extract_from_tag(mfile, b'version')

    if values == 'jar':
        return app_name + ".jar"
    elif values == 'name':
        return app_name
    elif values == 'version':
        return app_ver
    else:
        return ''
