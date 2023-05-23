package com.microsoft.azure.synapse.ml.nbtest

class ConfigurationLoaderTests extends NotebooksTestBase {
  test("CanLoadDefaultConfiguration") {
    ConfigurationLoader.getConfig().SynapseConfig.Notebooks.Paths
  }
}
