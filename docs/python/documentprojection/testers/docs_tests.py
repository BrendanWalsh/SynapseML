from framework import Tester, Notebook
from utils.logging import get_log

log = get_log(__name__)


class SynapseTester(Tester):
    def __init__(self):
        pass

    def test(self, notebook: Notebook) -> bool:
        return True


class DatabricksTester(Tester):
    def __init__(self):
        pass

    def test(self, notebook: Notebook) -> bool:
        return True


class NoopTester(Tester):
    def __init__(self):
        pass

    def test(self, notebook: Notebook) -> bool:
        return True
