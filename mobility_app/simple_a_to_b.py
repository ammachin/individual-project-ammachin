"""
Given coordinates sent from a phone,
fly a CrazyFlie from point A to point B in a direct line.
"""

import time

from cflib.crazyflie import Crazyflie
from cflib.crazyflie.syncCrazyflie import SyncCrazyflie

from mobility_class import MobilityClass
from mqtt_sub import MQTTSub

"""
Main function.
"""
if __name__ == "__main__":
    mobility = MobilityClass()
    mqtt_sub = MQTTSub()

    with SyncCrazyflie(mobility.URI, cf=Crazyflie(rw_cache="./cache")) as scf:
        log_conf = mobility.init_log_config(scf)

        # Checking deck is attached
        scf.cf.param.add_update_callback(group="deck", name="bcLoco", cb=mobility.param_deck_loco)
        time.sleep(1)

        print("Waiting for location to be received . . .")
        mqtt_sub.connect()

        # Assuming that when we get to here location will have been found
        print("Location received!")
        location = [None] * 3
        location[0], location[1], location[2] = map(float, mqtt_sub.current_msg.strip('()').split(','))

        # Set z to something speciifc for now
        location[2] = 0.5

        # Arm the CrazyFlie
        scf.cf.platform.send_arming_request(True)
        time.sleep(1.0)

        print("Initiating flight")
        mobility.take_off(scf, location)
        mobility.go_to(scf, location)
        mobility.land(scf)

        scf.cf.commander.send_stop_setpoint()
        scf.cf.commander.send_notify_setpoint_stop()
        print("Flight finished")
