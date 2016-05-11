package loea.sim.provider;

import java.util.Calendar;
import java.util.List;

import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;

import loea.sched.ProviderImporter;

public class ProviderTest {
	public static void main(String[] args) {

		try {
			// First step: Initialize the CloudSim package. It should be called
			// before creating any entities.
			int num_user = 1; // number of cloud users
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = true; // mean trace events

			// Initialize the CloudSim library
			CloudSim.init(num_user, calendar, trace_flag);

			List<Datacenter> centerList = ProviderImporter
					.XMLImporter("configs/provider1.xml");

			for (Datacenter center : centerList) {
				System.out.println(center.getName());
				for (Host host : center.getHostList()) {
					System.out.println("\t" + host.getId() + "\t"
							+ host.getTotalMips() + "\t" + host.getBw() + "\t"
							+ host.getRam() + "\t" + host.getStorage());

				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("Unwanted errors happen");
		}

	}
}
