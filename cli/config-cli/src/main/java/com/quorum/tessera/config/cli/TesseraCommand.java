package com.quorum.tessera.config.cli;

import com.quorum.tessera.ServiceLoaderUtil;
import com.quorum.tessera.cli.CliResult;
import com.quorum.tessera.cli.keypassresolver.CliKeyPasswordResolver;
import com.quorum.tessera.cli.keypassresolver.KeyPasswordResolver;
import com.quorum.tessera.cli.parsers.PidFileMixin;
import com.quorum.tessera.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "tessera",
        header = "Tessera - Privacy Manager for Quorum%n",
        descriptionHeading = "%nDescription: ",
        description = "Start a Tessera node.  Subcommands exist to manage Tessera encryption keys%n",
        parameterListHeading = "Parameters:%n",
        commandListHeading = "%nCommands:%n",
        optionListHeading = "Options:%n",
        abbreviateSynopsis = true)
public class TesseraCommand implements Callable<CliResult> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TesseraCommand.class);

    private final Validator validator;

    private final KeyPasswordResolver keyPasswordResolver;

    public TesseraCommand() {
        this(ServiceLoaderUtil.load(KeyPasswordResolver.class).orElse(new CliKeyPasswordResolver()));
    }

    private TesseraCommand(final KeyPasswordResolver keyPasswordResolver) {
        this.keyPasswordResolver = Objects.requireNonNull(keyPasswordResolver);
        this.validator =
            Validation.byDefaultProvider().configure().ignoreXmlConfiguration().buildValidatorFactory().getValidator();
    }

    @CommandLine.Option(
            names = {"--configfile", "-configfile"},
            description = "Path to node configuration file")
    public Config config;

    @CommandLine.Mixin
    private final PidFileMixin pidFileMixin = new PidFileMixin();

    @CommandLine.Option(
            names = {"-o", "--override"},
        description = "Override a value in the configuration. Can be used multiple times.")
    private final Map<String, String> overrides = new LinkedHashMap<>();

    @CommandLine.Option(
            names = {"-r", "--recover"},
            description = "Start Tessera in recovery mode")
    private boolean recover;

    @CommandLine.Mixin public DebugOptions debugOptions;

    // TODO(cjh) dry run option to print effective config to terminal to allow review of CLI overrides

    @Override
    public CliResult call() throws Exception {
        // we can't use required=true in the params for @Option as this also applies the requirement to all subcmds
        if (Objects.isNull(config)) {
            throw new NoTesseraConfigfileOptionException();
        }

        overrides.forEach((target, value) -> {
            LOGGER.debug("Setting : {} with value(s) {}", target, value);
            OverrideUtil.setValue(config, target, value);
            LOGGER.debug("Set : {} with value(s) {}", target, value);
        });

        if (recover) {
            config.setRecoveryMode(true);
        }

        final Set<ConstraintViolation<Config>> violations = validator.validate(config);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        keyPasswordResolver.resolveKeyPasswords(config);

        pidFileMixin.createPidFile();

        return new CliResult(0, false, config);
    }
}
