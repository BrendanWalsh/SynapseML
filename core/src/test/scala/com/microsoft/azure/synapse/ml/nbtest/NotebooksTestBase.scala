package com.microsoft.azure.synapse.ml.nbtest

import com.microsoft.azure.synapse.ml.core.test.base.TestBase

class NotebooksTestBase extends TestBase {
  lazy val config: NotebooksTestConfig = ConfigurationLoader.getConfig()
}
