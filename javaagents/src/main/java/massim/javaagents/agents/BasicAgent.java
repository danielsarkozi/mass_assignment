package massim.javaagents.agents;

import eis.iilang.*;
import massim.javaagents.MailService;

import java.util.List;

/**
 * A very basic agent.
 */
public class BasicAgent extends Agent {

    /**
     * Constructor.
     * @param name    the agent's name
     * @param mailbox the mail facility
     */
    public BasicAgent(String name, MailService mailbox) {
        super(name, mailbox);
    }

    @Override
    public void handlePercept(Percept percept) {
        System.out.println( this.getName() + " Basic percept" );
    }

    @Override
    public void handleMessage(Percept message, String sender) {
        System.out.println( this.getName() + " Basic message" );
    }

    @Override
    public Action step() {
        System.out.println( this.getName() + " Basic step" );
        List<Percept> percepts = getPercepts();
        percepts.stream()
                .filter(p -> p.getName().equals("step"))
                .findAny()
                .ifPresent(p -> {
                    Parameter param = p.getParameters().getFirst();
                    if(param instanceof Identifier) say("Step " + ((Identifier) param).getValue());
        });
        return new Action("move", new Identifier("n"));
    }
}
