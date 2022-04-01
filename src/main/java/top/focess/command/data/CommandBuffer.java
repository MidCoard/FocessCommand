package top.focess.command.data;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import top.focess.command.Command;

public class CommandBuffer extends DataBuffer<Command> {

    private final StringBuffer stringBuffer;

    public CommandBuffer(final int size) {
        this.stringBuffer = StringBuffer.allocate(size);
    }

    @NotNull
    @Contract("_ -> new")
    public static CommandBuffer allocate(final int size) {
        return new CommandBuffer(size);
    }

    @Override
    public void flip() {
        this.stringBuffer.flip();
    }

    @Override
    public void put(@NotNull final Command command) {
        this.stringBuffer.put(command.getName());
    }

    @NotNull
    @Override
    public Command get() {
        final String name = this.stringBuffer.get();
        for (final Command command : Command.getCommands())
            if (command.getName().equals(name))
                return command;
        throw new IllegalArgumentException("Command: " + name + " is not found");
    }

    @NotNull
    @Override
    public Command get(final int index) {
        final String name = this.stringBuffer.get(index);
        for (final Command command : Command.getCommands())
            if (command.getName().equals(name))
                return command;
        throw new IllegalArgumentException("Command: " + name + " is not found");
    }
}