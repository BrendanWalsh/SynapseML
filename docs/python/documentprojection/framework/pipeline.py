from typing import List
from documentprojection.channels import *
from documentprojection.channels import console
from documentprojection.testers import all_testers
from documentprojection.utils.notebook import get_mock_path
from documentprojection.utils.logging import get_log
from documentprojection.framework.objects import *
from documentprojection.utils.notebook import *

log = get_log(__name__)

class PipelineConfig():
    def __init__(self, dict: dict):
        self.__dict__.update(dict)
    
    test = None
    format = None
    publish = None
    metadata = None
    channel = None

class DocumentProjectionPipeline():
    def __init__(self,
                 testers: List[Tester] = [],
                 channels: List[Channel] = [],
                 config: PipelineConfig = PipelineConfig({})):
        
        self.config = config
        self.testers = [tester() for tester in testers]
        self.channels = [channel() for channel in channels]
        log.debug(f"""
        DocumentProjectionPipeline initialized with:
            Mode: {self.config}
            Testers: {self.testers}
            Channels: {self.channels}""")

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
        log.debug(f"""
        DocumentProjectionPipeline running with:
            Mode: {self.config}
            Notebooks: {notebooks}
            Testers: {self.testers}
            Channels: {self.channels}""")

        if notebooks is None or len(notebooks) == 0:
            log.warn("No notebooks provided. This pipeline will not do anything.")
            return

        log.debug("Running pipeline with {} notebooks:\n{}".format(len(notebooks), '\n'.join(map(repr, notebooks)))) # str format required to suppose "\" in expression

        if len(self.testers) == 0:
            log.warn("No testers registered. Nothing will be tested.")
        
        if len(self.channels) == 0:
            log.warn("No channels registered. Nothing will be formatted or published.")
        
        for notebook in notebooks:
            log.info(f"Processing notebook: {notebook}")
            failed_tests = []
            for tester in self.testers:
                if not tester.run_test(notebook):
                    failed_tests.append(tester)
            if (len(failed_tests) > 0):
                raise Exception("One or more tests failed: " + str(failed_tests))
            
            for channel in self.channels:
                log.debug(f"Processing channel: {channel.__class__.__name__}")

                document = channel.format(notebook)
                if self.config.publish:
                    channel.publish(document)
                else:
                    log.warn(f"PUBLISH mode not enabled. Skipping publish step.")
                
                if self.config.format:
                    log.info(f"Formatted content for {notebook}:\n{document.content}")
        
                if self.config.metadata:
                    metadata = channel.formatter.get_metadata(notebook)
                    log.info(f"Metadata for {notebook}:\n{metadata}")

def collect_notebooks(paths: List[str], recursive: bool) -> List[Notebook]:
    return [Notebook(nb) for nb in parse_notebooks(paths, recursive)]