from typing import List
from channels import *
from channels import console
from testers import all_testers
from utils.notebook import get_mock_path
from utils.logging import get_log
from .objects import *
from utils.notebook import *
from utils.parallelism import process_in_parallel

log = get_log(__name__)


class PipelineConfig:
    def __init__(self, dict: dict):
        self.__dict__.update(dict)

    test = None
    format = None
    publish = None
    metadata = None
    channel = None


class DocumentProjectionPipeline:
    def __init__(
        self,
        testers: List[Tester] = [],
        channels: List[Channel] = [],
        config: PipelineConfig = PipelineConfig({}),
    ):

        self.config = config
        self.testers = [tester() for tester in testers]
        self.channels = [channel() for channel in channels]
        log.debug(
            f"""
        DocumentProjectionPipeline initialized with:
            Mode: {self.config}
            Testers: {self.testers}
            Channels: {self.channels}"""
        )

    def register_testers(self, testers: List[Tester]):
        if len(testers) == 0:
            log.warn("Extending testers with an empty list")
        self.testers.extend([tester() for tester in testers])
        return self

    def register_channels(self, channels: List[Channel]):
        if len(channels) == 0:
            log.warn("Extending channels with an empty list")
        self.channels.extend([channel() for channel in channels])
        return self

    def run(self, notebooks: List[Notebook]) -> None:
        log.debug(
            f"""
        DocumentProjectionPipeline running with:
            Mode: {self.config}
            Notebooks: {notebooks}
            Testers: {self.testers}
            Channels: {self.channels}"""
        )

        if notebooks is None or len(notebooks) == 0:
            log.warn("No notebooks provided. This pipeline will not do anything.")
            return

        log.debug(
            "Running pipeline with {} notebooks:\n{}".format(
                len(notebooks), "\n".join(map(repr, notebooks))
            )
        )  # str format required to suppose "\" in expression

        if len(self.testers) == 0:
            log.warn("No testers registered. Nothing will be tested.")

        if len(self.channels) == 0:
            log.warn("No channels registered. Nothing will be formatted or published.")

        if not self.config.publish:
            log.warn(f"PUBLISH mode not enabled. Skipping publish step.")

        log.debug(f"Testing notebooks.")
        for notebook in notebooks:
            failed_tests = []
            for tester in self.testers:
                if not tester.run_test(notebook):
                    log.error(f"{tester} failed for {notebook}")
                    failed_tests.append(tester)
            if len(failed_tests) > 0:
                raise Exception("One or more tests failed: " + str(failed_tests))

        for channel in self.channels:
            log.info(f"Processing notebooks in parallel for channel: {repr(channel)}")
            formatted_documents = process_in_parallel(channel.format, notebooks)
            if self.config.publish:
                process_in_parallel(channel.publish, formatted_documents)
            if self.config.format:
                for i in range(len(notebooks)):
                    log.info(
                        "Formatted content for {}:\n{}".format(
                            notebooks[i], formatted_documents[i].content
                        )
                    )
                    log.info(f"End formatted content for {notebooks[i]}")

            if self.config.metadata:
                for notebook in notebooks:
                    metadata = channel.formatter.get_metadata(notebook)
                    log.info(metadata)


def collect_notebooks(paths: List[str], recursive: bool) -> List[Notebook]:
    return [Notebook(nb) for nb in parse_notebooks(paths, recursive)]
