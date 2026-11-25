// Command-line interface.
@ApplicationModule(
    allowedDependencies = {
      "common",
      "documents",
      "jobs",
      "extract",
      "normalize",
      "validate",
      "engine",
      "diff",
      "rules",
      "profile",
      "timeline",
      "reminders",
      "ask"
    })
package app.cairn.cli;

import org.springframework.modulith.ApplicationModule;
