"""
An adaptation of simple_a_to_b.py,
where we now have two points, plus hovering at each point
if passed as an argument

Point B will be passed as an argument for simplicity
"""

import sys
import time

from cflib.crazyflie import Crazyflie
from cflib.crazyflie.syncCrazyflie import SyncCrazyflie

from mobility_class import MobilityClass
from mqtt_sub import MQTTSub

"""
Main function.
"""

if __name__ == "__main__":
    # Handling command line arguments
    if(len(sys.argv) == 3):
        point_b = sys.argv[1] 
        if(sys.argv[2] == "hover"):
            hover = True
        else:
            print("Incorrect arguments")
            sys.exit(1)
    elif(len(sys.argv) == 2):
        point_b = sys.argv[1]
        hover = False
    else:
        print("Incorrect arguments")
        sys.exit(1)

    location_b = [None] * 3
    location_b[0], location_b[1], location_b[2] = map(float, point_b.strip('()').split(','))

    mobility = MobilityClass()
    mqtt_sub = MQTTSub()

    with SyncCrazyflie(mobility.URI, cf=Crazyflie(rw_cache="./cache")) as scf:
        log_conf = mobility.init_log_config(scf)

        # Checking deck is attached
        scf.cf.param.add_update_callback(group="deck", name="bcLoco", cb=mobility.param_deck_loco)
        time.sleep(1)

        print("Wating for location to be received . . .")
        mqtt_sub.connect()

        # Assuming that when we get to here location will have been found
        print("Location received!")
        location_c = [None] * 3
        location_c[0], location_c[1], location_c[2] = map(float, mqtt_sub.current_msg.strip('()').split(','))

        # Arm the CrazyFlie
        scf.cf.platform.send_arming_request(True)
        time.sleep(1.0)

        print("Initating flight")

        mobility.take_off(scf, location_b)
        if(hover):
            # Setting any hovering to 5 seconds for now
            mobility.hover(scf, 5)
        mobility.go_to(scf, location_b)
        if(hover):
            mobility.hover(scf, 5)
        mobility.go_to(scf, location_c)
        if(hover):
            mobility.hover(scf, 5)
        mobility.land(scf)

        scf.cf.commander.send_stop_setpoint()
        scf.cf.commander.send_notify_setpoint_stop()
        print("Flight finished")

        

