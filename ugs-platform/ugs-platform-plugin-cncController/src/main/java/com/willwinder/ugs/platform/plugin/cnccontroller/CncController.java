/*
 * Copyright (C) 2020 arne
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.willwinder.ugs.platform.plugin.cnccontroller;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.willwinder.ugs.nbp.lib.services.ActionReference;
import com.willwinder.ugs.nbp.lib.services.ActionRegistrationService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import net.java.games.input.Component;
import net.java.games.input.Component.Identifier.Axis;
import net.java.games.input.Component.Identifier.Button;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Event;
import net.java.games.input.EventQueue;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;

/**
 * This class encapsulates the cnc controller.
 * @author arne
 * 
 */
public class CncController {
    
    private static final Logger log = Logger.getLogger(CncController.class.getSimpleName());
    private ExecutorService runThread;
    private boolean isRunning = false;
    ArrayList<Controller> muhiControllers = new ArrayList<Controller>();
    private ActionRegistrationService actionRegistrationService;

    
    private ActionReference homeAction;
    private ActionReference pauseAction;
    private ActionReference startAction;
    private ActionReference resetAction;
    private ActionReference stopAction;
    
    public CncController(){
        log.info("Frickelcnc Controller loading...");
        Controller[] controllers = ControllerEnvironment
                .getDefaultEnvironment().getControllers();
       
        
        for(Controller c : controllers)
        {
            if(c.getName().equals("MUHI"))
            {
                log.info("Found muhi controller");
                muhiControllers.add(c);                
            }
        }

        if (muhiControllers.isEmpty()) {
            log.warning("Found no cnc controller. Controller plugin disabled");
            return;
        }
        
        actionRegistrationService = Lookup.getDefault().lookup(ActionRegistrationService.class);    
        runThread = Executors.newSingleThreadExecutor();
        runThread.execute(this::run);
    }
    
    
    ActionReference waitForAndGetAction(String id)
    {
        log.info("Waiting for Action: " + id);
        Optional<ActionReference> act;
        do{
            act = actionRegistrationService.getActionById(id);
            try {
                Thread.sleep(20);
            } catch (InterruptedException ex) {
                Exceptions.printStackTrace(ex);
            }
        }while(act.isEmpty());
        
        return act.get();
    }
    
    void loadActions()
    {
        homeAction = waitForAndGetAction("Actions/Machine/com-willwinder-ugs-nbp-core-actions-HomeAction.instance");
        pauseAction = waitForAndGetAction("Actions/Machine/com-willwinder-ugs-nbp-core-actions-PauseAction.instance");
        startAction = waitForAndGetAction("Actions/Machine/com-willwinder-ugs-nbp-core-actions-StartAction.instance");
        resetAction = waitForAndGetAction("Actions/Machine/com-willwinder-ugs-nbp-core-actions-SoftResetAction.instance");
        stopAction = waitForAndGetAction("Actions/Machine/com-willwinder-ugs-nbp-core-actions-StopAction.instance");

        //nullen
        // Actions/Machine/com-willwinder-ugs-nbp-core-actions-ResetXCoordinatesToZeroAction.instance
        // Actions/Machine/com-willwinder-ugs-nbp-core-actions-ResetYCoordinatesToZeroAction.instance
        // Actions/Machine/com-willwinder-ugs-nbp-core-actions-ResetZCoordinatesToZeroAction.instance
        // Actions/Machine/com-willwinder-ugs-nbp-core-actions-ResetCoordinatesToZeroAction.instance
        
       /*
        --- Actions/Machine/com.willwinder.ugs.nbp.core.services.JogActionService.JogSizeAction.decrease.feed.instance
--- Actions/Machine/com.willwinder.ugs.nbp.core.services.JogActionService.JogSizeAction.decrease.instance
--- Actions/Machine/com.willwinder.ugs.nbp.core.services.JogActionService.JogSizeAction.decrease.z.instance
--- Actions/Machine/com.willwinder.ugs.nbp.core.services.JogActionService.JogSizeAction.divide.feed.instance
--- Actions/Machine/com.willwinder.ugs.nbp.core.services.JogActionService.JogSizeAction.divide.instance
--- Actions/Machine/com.willwinder.ugs.nbp.core.services.JogActionService.JogSizeAction.divide.z.instance
--- Actions/Machine/com.willwinder.ugs.nbp.core.services.JogActionService.JogSizeAction.inch.instance
--- Actions/Machine/com.willwinder.ugs.nbp.core.services.JogActionService.JogSizeAction.increase.feed.instance
--- Actions/Machine/com.willwinder.ugs.nbp.core.services.JogActionService.JogSizeAction.increase.instance
--- Actions/Machine/com.willwinder.ugs.nbp.core.services.JogActionService.JogSizeAction.increase.z.instance
--- Actions/Machine/com.willwinder.ugs.nbp.core.services.JogActionService.JogSizeAction.mm.instance
--- Actions/Machine/com.willwinder.ugs.nbp.core.services.JogActionService.JogSizeAction.multiply.feed.instance
--- Actions/Machine/com.willwinder.ugs.nbp.core.services.JogActionService.JogSizeAction.multiply.instance
--- Actions/Machine/com.willwinder.ugs.nbp.core.services.JogActionService.JogSizeAction.multiply.z.instance
--- Actions/Machine/com.willwinder.ugs.nbp.core.services.JogActionService.JogSizeAction.toggle.instance
--- Actions/Machine/com.willwinder.ugs.nbp.core.services.JogActionService.xPlus.instance
--- Actions/Machine/com.willwinder.ugs.nbp.core.services.JogActionService.xPlus.yPlus.instance
--- Actions/Machine/com.willwinder.ugs.nbp.core.services.JogActionService.xPlus.yMinus.instance
--- Actions/Machine/com.willwinder.ugs.nbp.core.services.JogActionService.xMinus.instance
--- Actions/Machine/com.willwinder.ugs.nbp.core.services.JogActionService.xMinus.yPlus.instance
--- Actions/Machine/com.willwinder.ugs.nbp.core.services.JogActionService.xMinus.yMinus.instance
--- Actions/Machine/com.willwinder.ugs.nbp.core.services.JogActionService.yPlus.instance
--- Actions/Machine/com.willwinder.ugs.nbp.core.services.JogActionService.yMinus.instance
--- Actions/Machine/com.willwinder.ugs.nbp.core.services.JogActionService.zPlus.instance
--- Actions/Machine/com.willwinder.ugs.nbp.core.services.JogActionService.zMinus.instance
        
        --- Actions/Machine/com.willwinder.ugs.nbp.core.services.JogActionService.JogSizeActionxy.0001.instance
--- Actions/Machine/com.willwinder.ugs.nbp.core.services.JogActionService.JogSizeActionxy.001.instance
--- Actions/Machine/com.willwinder.ugs.nbp.core.services.JogActionService.JogSizeActionxy.01.instance
--- Actions/Machine/com.willwinder.ugs.nbp.core.services.JogActionService.JogSizeActionxy.1.instance
--- Actions/Machine/com.willwinder.ugs.nbp.core.services.JogActionService.JogSizeActionxy.10.instance
--- Actions/Machine/com.willwinder.ugs.nbp.core.services.JogActionService.JogSizeActionz.0001.instance
--- Actions/Machine/com.willwinder.ugs.nbp.core.services.JogActionService.JogSizeActionz.001.instance
--- Actions/Machine/com.willwinder.ugs.nbp.core.services.JogActionService.JogSizeActionz.01.instance
--- Actions/Machine/com.willwinder.ugs.nbp.core.services.JogActionService.JogSizeActionz.1.instance
--- Actions/Machine/com.willwinder.ugs.nbp.core.services.JogActionService.JogSizeActionz.10.instance
      
--- Actions/Overrides/com.willwinder.ugs.nbp.core.services.OverrideAction.feedOvrFinePlus.instance
--- Actions/Overrides/com.willwinder.ugs.nbp.core.services.OverrideAction.feedOvrCoarsePlus.instance
--- Actions/Overrides/com.willwinder.ugs.nbp.core.services.OverrideAction.feedOvrFineMinus.instance
--- Actions/Overrides/com.willwinder.ugs.nbp.core.services.OverrideAction.feedOvrCoarseMinus.instance
--- Actions/Overrides/com.willwinder.ugs.nbp.core.services.OverrideAction.feedOvrReset.instance
--- Actions/Overrides/com.willwinder.ugs.nbp.core.services.OverrideAction.toogleFloodCoolant.instance
--- Actions/Overrides/com.willwinder.ugs.nbp.core.services.OverrideAction.toggleMistCoolant.instance
--- Actions/Overrides/com.willwinder.ugs.nbp.core.services.OverrideAction.rapidOvrReset.instance
--- Actions/Overrides/com.willwinder.ugs.nbp.core.services.OverrideAction.rapidOvrLow.instance
--- Actions/Overrides/com.willwinder.ugs.nbp.core.services.OverrideAction.rapidOvrMedium.instance
--- Actions/Overrides/com.willwinder.ugs.nbp.core.services.OverrideAction.toggleSpindle.instance
--- Actions/Overrides/com.willwinder.ugs.nbp.core.services.OverrideAction.spindleOvrFinePlus.instance
--- Actions/Overrides/com.willwinder.ugs.nbp.core.services.OverrideAction.spindleOvrCoarsePlus.instance
--- Actions/Overrides/com.willwinder.ugs.nbp.core.services.OverrideAction.spindleOvrFineMinus.instance
--- Actions/Overrides/com.willwinder.ugs.nbp.core.services.OverrideAction.spindleOvrCoarseMinus.instance
--- Actions/Overrides/com.willwinder.ugs.nbp.core.services.OverrideAction.spindleOvrReset.instance
        
*/

    }    
    
    public void stop()
    {
        if(isRunning)
        {
            isRunning = false;
            runThread.shutdown();
            try {
                runThread.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }
        
    
    private void handleButton(Button b, float value)
    {
        if(b.equals(Button._12)) // start
        {
            System.out.println("Start button");
        }
        else if(b.equals(Button._10)) // Stop
        {
            
        }
        else if(b.equals(Button._14)) // Pause
        {
            
        }
        else if(b.equals(Button._13)) // Reset
        {
            
        }
        else if(b.equals(Button._8)) // Home
        {
            
        }
        else if(b.equals(Button._11)) // jog mode
        {
            
        }
        else if(b.equals(Button._22)) // Vorschub
        {
            
        }
        else if(b.equals(Button._21)) // spindel
        {
            
        }
        else if(b.equals(Button._0)) // Achse 0000
        {
            
        }
        else if(b.equals(Button._9)) // Achsen 000
        {
            
        }
        else
        {
            log.warning("Unknown button: " + b.toString());
        }

    }
    
    private void handleAxis(Axis a, float value)
    {
        System.out.println("Axis " + a.toString() + " v: " + value);
        //TODO value can be any float (number of ticks since last event)
        //TODO beim boot up kommen werte:
        //  Axis slider v: -0.007827878
        //  Axis z v: -0.007827878
        //  Axis y v: -0.007827878
        //  Axis x v: -0.007827878
    }
    
    private void handleEvent(Event e)
    {
        Component c = e.getComponent();
        if(c.getIdentifier() instanceof Button)
        {
            handleButton((Button)c.getIdentifier(), e.getValue());
        }
        else if(c.getIdentifier() instanceof Axis)
        {
            handleAxis((Axis)c.getIdentifier(), e.getValue());
        }
    }
    
    private void handleEvents(Controller c)
    {
        c.poll();
        EventQueue queue = c.getEventQueue();
        Event event = new Event();
        while (queue.getNextEvent(event)) {
            handleEvent(event);
        }
    }
    
    private void run() {
        isRunning = true;
        
        loadActions();      
                
        while (isRunning) {
            for(Controller c : muhiControllers)
            {
                handleEvents(c);
            }
            /*
            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                // Never mind this
            }
            */
        }
    }
    
}
