package org.opennms.ng.services.eventd;

import java.sql.SQLException;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.opennms.netmgt.model.events.EventProcessor;
import org.opennms.netmgt.xml.event.Event;
import org.opennms.netmgt.xml.event.Log;
import org.opennms.netmgt.xml.event.Parm;
import org.opennms.ng.services.eventconfig.EventExpander;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventTranslator implements Processor {

    private List<EventProcessor> m_eventProcessors;
    private EventExpander eventExpander;
    Logger log = LoggerFactory.getLogger(EventTranslator.class);

    @Override
    public void process(Exchange exchange) throws Exception {
        System.out.println("Event Received " + exchange.getIn().getBody());

        //TODO - Move to a central JaxbContext.
        Log xmlEvent = exchange.getIn().getBody(Log.class);

        for (final Event event : xmlEvent.getEvents().getEventCollection()) {
            if (log.isDebugEnabled()) {
                // Log the uei, source, and other important aspects
                final String uuid = event.getUuid();
                log.debug("Event {");
                log.debug("  uuid  = " + (uuid != null && uuid.length() > 0 ? uuid : "<not-set>"));
                log.debug("  uei   = " + event.getUei());
                log.debug("  src   = " + event.getSource());
                log.debug("  iface = " + event.getInterface());
                log.debug("  time  = " + event.getTime());
                if (event.getParmCollection().size() > 0) {
                    log.debug("  parms {");
                    for (final Parm parm : event.getParmCollection()) {
                        if ((parm.getParmName() != null) && (parm.getValue().getContent() != null)) {
                            log.debug("    (" + parm.getParmName().trim() + ", " + parm.getValue().getContent().trim() + ")");
                        }
                    }
                    log.debug("  }");
                }
                log.debug("}");
            }

            for (final EventProcessor eventProcessor : m_eventProcessors) {
                try {
                    eventProcessor.process(xmlEvent.getHeader(), event);
                } catch (SQLException e) {
                    log.warn(
                        "Unable to process event using processor " + eventProcessor + "; not processing with any later processors.  Exception: " + e,
                        e);
                    break;
                } catch (Throwable t) {
                    log.warn("Unknown exception processing event with processor " + eventProcessor
                        + "; not processing with any later processors.  Exception: " + t, t);
                    break;
                }
            }
        }
    }
}