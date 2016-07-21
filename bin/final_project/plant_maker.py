#!/usr/local/bin/python3.5
from subprocess import Popen


def main():
    pid1 = Popen(["/root/java final_project.test", ]).pid
    pid2 = Popen(["/root/java final_project.Chlorinator", ]).pid
    pid3 = Popen(["/root/java final_project.Hazmatter", ]).pid
    pid4 = Popen(["/root/python3 final_project/sludger.py", ]).pid
    #TODO while true make sure all are running

if __name__ == "__main__":
    main();