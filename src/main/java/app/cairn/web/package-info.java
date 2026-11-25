// Web pages and HTTP API.
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
package app.cairn.web;

import org.springframework.modulith.ApplicationModule;
