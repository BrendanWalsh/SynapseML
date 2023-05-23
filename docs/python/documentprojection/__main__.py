from __future__ import absolute_import

from .utils.reflection import *
from .utils.logging import *
from .utils.notebook import *
from .framework.pipeline import *
from .channels import default_channels

import re

log = get_log(__name__)


def get_channel_map(custom_channels_folder, cwd):

    sys.path.insert(0, "documentprojection")

    def camel_to_snake(name):
        s1 = re.sub("(.)([A-Z][a-z]+)", r"\1_\2", name)
        s2 = re.sub("([a-z0-9])([A-Z])", r"\1_\2", s1).lower()
        return s2.replace("_channel", "")

    channels = default_channels.copy()
    if custom_channels_folder is not None and len(custom_channels_folder) > 0:
        channels.extend(get_channels_from_dir(custom_channels_folder, cwd))

    channel_map = {
        k: v
        for k, v in [
            (camel_to_snake(channel.__name__), channel) for channel in channels
        ]
    }
    return channel_map


def parse_args():
    log_level_choices = ["debug", "info", "warn", "error", "critical"]

    import argparse

    parser = argparse.ArgumentParser(description="Document Projection Pipeline")
    parser.add_argument(
        "project_root",
        metavar="ROOT",
        type=str,
        help="the root directory of the project",
        default=".",
    )
    parser.add_argument(
        "notebooks",
        metavar="N",
        type=str,
        nargs="*",
        help="a notebook or folder of notebooks to project",
    )
    parser.add_argument(
        "-f", "--format", action="store_true", default=False, help="run only formatters"
    )
    parser.add_argument(
        "-p",
        "--publish",
        action="store_true",
        default=False,
        help="run publishers. forces -t and -f.",
    )
    parser.add_argument(
        "-m",
        "--metadata",
        action="store_true",
        default=False,
        help="format documents and only output metadata of formated documents.",
    )
    parser.add_argument(
        "-r",
        "--recursive",
        action="store_true",
        default=False,
        help="recursively scan for notebooks in the given directory.",
    )
    parser.add_argument(
        "-l", "--list", action="store_true", default=False, help="list notebooks"
    )
    parser.add_argument(
        "-c",
        "--channels",
        default="console",
        type=str,
        help="A channel or comma-separated list of channels through which the notebook(s) should be processed. defaults to console if not specified.",
    )
    parser.add_argument(
        "--customchannels",
        type=str,
        default=None,
        help="A folder containing custom channel implementations.",
    )
    parser.add_argument(
        "-v",
        "--loglevel",
        choices=log_level_choices,
        default="info",
        help="set log level",
    )
    return parser.parse_args()


def run():
    args = parse_args()
    config_log(args.loglevel)
    log.debug("script executed with args: {}".format(args))

    args.project_root = os.path.abspath(args.project_root)

    if len(args.notebooks) == 0:
        log.warn("No notebooks specified. Using sample notebook.")
        args.notebooks = [get_mock_path()]

    notebooks = collect_notebooks(args.notebooks, args.recursive)
    if args.list:
        log.info("Notebooks found:\n{}".format("\n".join(map(repr, notebooks))))
        log.info("--info specified. Will not process any notebooks.")
        return
    log.debug("notebooks specified: {}".format(notebooks))

    pipeline = DocumentProjectionPipeline(config=PipelineConfig(vars(args)))
    channels = []
    channel_map = get_channel_map(args.customchannels, args.project_root)
    for channel in args.channels.split(","):
        if channel not in channel_map:
            log.critical(
                f"Channel '{channel}' not found. If this is a custom channel, make sure it is in the custom channels folder and that the folder is specified with the --customchannels argument."
            )
            return

        channels.append(channel_map[channel])
    pipeline.register_channels(channels)
    pipeline.run(notebooks)


run()
