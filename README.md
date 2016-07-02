# Task scheduling for SKA

## Critical path Aware EFT (CAEFT)

Critical path Aware EFT (Earliest Finish Time) algorithm, CAEFT for short. Related files: simulator/ScalableSimulatorCAEFT.java and scheduler/TaskBrokerCAEFT.java 

In CAEFT, subtasks that on the critical path of a task are finished as soon as possible. High level scheduler (i.e. Task scheduler) communicate with low level schedulers (e.g. VM scheduler and subtask scheduler) to acquire capability and availability of computing resources.

To run the simulator with this algorithm, generate jar package and try the following commands:

`cd $(compsched)`
`java -cp target/*;lib/* loea.sched.simulator.ScalableSimulatorCAEFT -p configs\prov_h4.xml -v configs\vm_v4.xml -t configs\task_s01_t1_st1000_e10000.xml`


## Genetic Algorithm (GA)

Related files: simulator/ScalableSimulatorGA.java and scheduler/TaskBrokerGA.java

Cloudsim acts as a fitness function of GA.

## Round Robin (RR)

Related files: simulator/ScalableSimulatorRR.java and scheduler/TaskBrokerRR.java

## Simulation input random generators

TaskGenerator.py
VMGenerator.py

