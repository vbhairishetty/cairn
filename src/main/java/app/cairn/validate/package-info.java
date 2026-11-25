// Validates normalized plans and quarantines bad values.
@ApplicationModule(allowedDependencies = {"common", "normalize"})
package app.cairn.validate;

import org.springframework.modulith.ApplicationModule;
