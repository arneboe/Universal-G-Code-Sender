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
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javax.swing.Action;
import net.java.games.input.Component;
import net.java.games.input.Component.Identifier.Axis;
import net.java.games.input.Component.Identifier.Button;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Event;
import net.java.games.input.EventQueue;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;



enum SelectedAxis
{
    X,Y,Z,A,NONE;
}

enum StepSize
{
    SIZE_10,SIZE_1,SIZE_01,SIZE_001;
}

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

    private SelectedAxis selectedAxis = SelectedAxis.NONE;
    
    private ActionReference homeAction;
    private ActionReference pauseAction;
    private ActionReference startAction;
    private ActionReference resetAction;
    private ActionReference stopAction;
    private ActionReference jogSizeXY10;
    private ActionReference jogSizeXY1;
    private ActionReference jogSizeXY01;
    private ActionReference jogSizeXY001;
    private ActionReference jogStepXPlus;
    private ActionReference jogStepYPlus;
    private ActionReference jogStepZPlus;
    private ActionReference jogStepXMinus;
    private ActionReference jogStepYMinus;
    private ActionReference jogStepZMinus;
    private ActionReference feedOverrideFinePlus;
    private ActionReference feedOverrideFineMinus;
    private ActionReference feedOverrideReset;
      
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
    
    
    void execAction(ActionReference ar)
    {
        Action action = ar.getAction();
        if(action.isEnabled())
        {
            action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));
        }
    }
    
    void execAction(ActionReference ar, int times)
    {
        Action action = ar.getAction();
        if(action.isEnabled())
        {
            for(int i = 0; i < times; i++)
            {
                action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));
            }
        }
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
        jogSizeXY001 = waitForAndGetAction("Actions/Machine/com.willwinder.ugs.nbp.core.services.JogActionService.JogSizeActionxy.001.instance");
        jogSizeXY01 = waitForAndGetAction("Actions/Machine/com.willwinder.ugs.nbp.core.services.JogActionService.JogSizeActionxy.01.instance");
        jogSizeXY1 = waitForAndGetAction("Actions/Machine/com.willwinder.ugs.nbp.core.services.JogActionService.JogSizeActionxy.1.instance");
        jogSizeXY10 = waitForAndGetAction("Actions/Machine/com.willwinder.ugs.nbp.core.services.JogActionService.JogSizeActionxy.10.instance");
        jogStepXPlus = waitForAndGetAction("Actions/Machine/com.willwinder.ugs.nbp.core.services.JogActionService.xPlus.instance");
        jogStepYPlus = waitForAndGetAction("Actions/Machine/com.willwinder.ugs.nbp.core.services.JogActionService.yPlus.instance");
        jogStepZPlus = waitForAndGetAction("Actions/Machine/com.willwinder.ugs.nbp.core.services.JogActionService.zPlus.instance");
        jogStepXMinus = waitForAndGetAction("Actions/Machine/com.willwinder.ugs.nbp.core.services.JogActionService.xMinus.instance");
        jogStepYMinus = waitForAndGetAction("Actions/Machine/com.willwinder.ugs.nbp.core.services.JogActionService.yMinus.instance");
        jogStepZMinus = waitForAndGetAction("Actions/Machine/com.willwinder.ugs.nbp.core.services.JogActionService.zMinus.instance");
        feedOverrideFinePlus = waitForAndGetAction("Actions/Overrides/com.willwinder.ugs.nbp.core.services.OverrideAction.feedOvrFinePlus.instance");
        feedOverrideFineMinus = waitForAndGetAction("Actions/Overrides/com.willwinder.ugs.nbp.core.services.OverrideAction.feedOvrFineMinus.instance");
        feedOverrideReset = waitForAndGetAction("Actions/Overrides/com.willwinder.ugs.nbp.core.services.OverrideAction.feedOvrReset.instance");
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
      c
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
        
    private void selectStepSize(StepSize size)
    {
        switch(size)
        {
            case SIZE_001:
                execAction(jogSizeXY001);
                break;
            case SIZE_01:
                execAction(jogSizeXY01);
                break;
            case SIZE_1:
                execAction(jogSizeXY1);
                break;
            case SIZE_10:
                execAction(jogSizeXY10);
                break;
            default:
                log.warning("illegal case");
        }
    }
    
    private void handleButton(Button b, float value)
    {
        
        //log.info("Button: " + b.toString() + "v: " + value);
        
        if(b.equals(Button._12)) // start
        {
            if(value == 1.0)
                log.info("Button: Start");
        }
        else if(b.equals(Button._10)) // Stop
        {
            if(value == 1.0)
                log.info("Button: Stop");
        }
        else if(b.equals(Button._14)) // Pause
        {
            if(value == 1.0)
                log.info("Button: Pause");
        }
        else if(b.equals(Button._13)) // Reset
        {
            if(value == 1.0)
                log.info("Button: Reset");
        }
        else if(b.equals(Button._8)) // Home
        {
            if(value == 1.0)
            {
                log.info("Button: Home");
                execAction(homeAction);
            }
        }
        else if(b.equals(Button._11)) // jog mode
        {
            if(value == 1.0)
                log.info("Button: Jog Mode");
        }
        else if(b.equals(Button._22)) // Vorschub
        {
            if(value == 1.0)
            {
                execAction(feedOverrideReset);
                log.info("Button: Vorschub reset");
            }
        }
        else if(b.equals(Button._21)) // spindel
        {
            if(value == 1.0)
                log.info("Button: Spindel");
            
        }
        else if(b.equals(Button._0)) // Achse 0000
        {
            if(value == 1.0)
                log.info("Button: Achse 000");
        }
        else if(b.equals(Button._9)) // Achsen 000
        {
            if(value == 1.0)
                log.info("Button: Achsen 000");
        }
        else if(b.equals(Button._5))
        {
            if(value == 1.0)
            {
                log.info("Step select: 1");
                selectStepSize(StepSize.SIZE_1);
            }
            else if(value == 0.0)
            {
                log.info("Step select: 10");
                selectStepSize(StepSize.SIZE_10);
            }
            else {
                log.warning("Button 5 unknown value: " + value);
            }
        }
        else if(b.equals(Button._6))
        {
            if(value == 1.0)
            {
                log.info("Step select: 0.1");
                selectStepSize(StepSize.SIZE_01);
            }
            else if(value != 0.0)
            {
                log.warning("Button 6 unknown value: " + value);
            }
        }
        else if(b.equals(Button._3))
        {
            if(value == 1.0)
            {
                log.info("Step select: 0.01");
                selectStepSize(StepSize.SIZE_001);
            }
            else if(value != 0.0)
            {
                log.warning("Button 3 unknown value: " + value);
            }
        }
        else if(b.equals(Button._4))
        {
            if(value == 1.0)
            {
                log.info("Axis select: A");
                selectedAxis = SelectedAxis.A;
            }
            else if(value != 0.0)
            {
                log.warning("Button 4 unknown value: " + value);
            }
        }
        else if(b.equals(Button._1))
        {
            if(value == 1.0)
            {
                log.info("Axis select: Z");
                selectedAxis = SelectedAxis.Z;
            }
            else if(value != 0.0)
            {
                log.warning("Button 1 unknown value: " + value);
            }
        }
        else if(b.equals(Button._2))
        {
            if(value == 1.0)
            {
                log.info("Axis select: Y");
                selectedAxis = SelectedAxis.Y;
            }
            else if(value != 0.0)
            {
                log.warning("Button 2 unknown value: " + value);
            }
        }
        else if(b.equals(Button._7))
        {
            if(value == 1.0)
            {
                log.info("Axis select: X");
                selectedAxis = SelectedAxis.X;
            }
            else if(value != 0.0)
            {
                log.warning("Button 7 unknown value: " + value);
            }
        }
        else
        {
            log.warning("Unknown button: " + b.toString());
        }
    }
    
    private void handleAxis(Axis a, float value)
    {
        if(value < 0.5 && value > -0.5)
        {
            //at startup the controller sends: 
            //  Axis slider v: -0.007827878
            //  Axis z v: -0.007827878
            //  Axis y v: -0.007827878
            //  Axis x v: -0.007827878
            //this needs to be ignored
            return;
        }

        if(a.equals(Axis.X)) //the big wheel
        {
            if(selectedAxis == SelectedAxis.X)
            {
                if(value > 0)
                    execAction(jogStepXPlus, (int)(value + 0.1)); //+0.1 to avoid wrong rounding
                else
                    execAction(jogStepXMinus, (int)(value * -1 + 0.1)); //+0.1 to avoid wrong rounding
            }
            else if(selectedAxis == SelectedAxis.Y)
            {
                if(value > 0)
                    execAction(jogStepYPlus, (int)(value + 0.1)); //+0.1 to avoid wrong rounding
                else
                    execAction(jogStepYMinus, (int)(value * -1 + 0.1)); //+0.1 to avoid wrong rounding
            }
            else if(selectedAxis == SelectedAxis.Z)
            {
                if(value > 0)
                    execAction(jogStepZPlus, (int)(value + 0.1)); //+0.1 to avoid wrong rounding
                else
                    execAction(jogStepZMinus, (int)(value * -1 + 0.1)); //+0.1 to avoid wrong rounding
            }
        }
        else if(a.equals(Axis.Y)) //vorschub
        {
            if(value > 0)
                execAction(feedOverrideFinePlus, (int)(value + 0.1)); //+0.1 to avoid wrong rounding
            else
                execAction(feedOverrideFineMinus, (int)(value * -1 + 0.1)); //+0.1 to avoid wrong rounding
        }
        //Z = spindel speed
        

        //TODO value can be any float (number of ticks since last event)
        //TODO beim boot up kommen werte:

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
        
        //TODO remove this sleep. fix the race instead
        //TODO figure out how to wait until application has been initialized
        try {
            Thread.sleep(8000);
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        }
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
