package org.globusonline.commands;


public abstract class JGOCommand {

	public static String getPath(String username, String op, String[] opArgs)
			throws Exception
			{
		if ((op == null) || (username == null))
		{
			return null;
		}
		StringBuffer sb = new StringBuffer();
		if (op.equals("tasksummary"))
		{
			sb.append("/tasksummary");
		}
		else if (op.equals("task"))
		{
			if (opArgs != null)
			{
				sb.append("/task(");
				sb.append(opArgs[0]);
				sb.append(")");
			}
			else
			{
				sb.append("/task");
			}
		}
		else if (op.equals("endpoint-list"))
		{
			// v0.9
			// sb.append("/user(");
			// sb.append(username);
			// sb.append(")/endpoint?limit=100");

			// v0.10
			sb.append("/endpoint_list?limit=100");
		}
		else if (op.equals("endpoint-add"))
		{
			sb.append("/endpoint");

		}
		else if (op.equals("endpoint-remove"))
		{
			if (opArgs != null)
			{
				String endpoint = opArgs[0];
				// v0.9
				// int pos = endpoint.indexOf("#");
				// if (pos != -1)
				// {
				//     String user = endpoint.substring(0, pos);
				//     String ep = endpoint.substring(pos +1);
				//     sb.append("/user(");
				//     sb.append(user);
				//     sb.append(")/endpoint(");
				//     sb.append(ep);
				//     sb.append(")");
				// }
				// else
				// {
				//     sb.append("/user(");
				//     sb.append(username);
				//     sb.append(")/endpoint(");
				//     sb.append(endpoint);
				//     sb.append(")");
				// }

				// v0.10
				int pos = endpoint.indexOf("#");
				if (pos != -1)
				{
					endpoint = endpoint.substring(pos + 1);
				}
				sb.append("/endpoint/");
				sb.append(endpoint);
			}
			else
			{
				throw new Exception("endpoint-remove requires an endpoint-name]");
			}
		}
		else if (op.equals("activate"))
		{
			if (opArgs != null)
			{
				String ep = opArgs[0];

				int pos = ep.indexOf("#");
				if (pos == -1)
				{
					// v0.9
					// sb.append("/endpoint(");
					// sb.append(ep);
					// sb.append(")/activation_requirements");

					// v0.10
					sb.append("endpoint/");
					sb.append(ep);
					sb.append("/activation_requirements");
				}
				else
				{
					// v0.9
					// String user = ep.substring(0, pos);
					// String newep = ep.substring(pos + 1);
					// sb.append("user(");
					// sb.append(user);
					// sb.append(")/endpoint(");
					// sb.append(newep);
					// sb.append(")/activation_requirements");

					// v0.10
					String newep = ep.substring(pos + 1);
					sb.append("endpoint/");
					sb.append(newep);
					sb.append("/activation_requirements");
				}
			}
			else
			{
				throw new Exception("Activate requires an endpoint [see Usage]");
			}
		}
		else if (op.equals("transfer"))
		{
			if ((opArgs != null) && (opArgs.length > 1))
			{
				// v0.9
				//sb.append("/transfer/generate_id");

				// v0.10
				sb.append("/transfer/submission_id");
			}
			else
			{
				throw new Exception("transfer requires both a source and destination path");
			}
		}
		else if (op.equals("__internal-endpoint-list"))
		{
			if (opArgs != null)
			{
				String ep = opArgs[0];
				int pos = ep.indexOf("#");
				if (pos == -1)
				{
					sb.append("/user(");
					sb.append(username);
					sb.append(")/endpoint(");
					sb.append(opArgs[0]);
					sb.append(")");
				}
				else
				{
					String user = ep.substring(0, pos);
					String newep = ep.substring(pos + 1);
					sb.append("/user(");
					sb.append(user);
					sb.append(")/endpoint(");
					sb.append(newep);
					sb.append(")");
				}
			}
			else
			{
				throw new Exception("__internal-endpoint- requires an endpoint");
			}
		}
		else
		{
			return null;
		}
		return sb.toString();
			}

	public static String opArgGetValue(String[] args, String key) {
		String value = null;
		if (args != null) {
			for (int i = 0; i < args.length; i++) {
				if (args[i].equals(key)) {
					if ((i + 1) < args.length) {
						value = args[++i];
					}
					break;
				}
			}
		}
		return value;
	}

	public static boolean opArgHasValue(String[] args, String value) {
		boolean found = false;
		if (args != null) {
			for (String tmp : args) {
				if (tmp.equals(value)) {
					found = true;
					break;
				}
			}
		}
		return found;
	}

	public abstract void process() throws Exception;

}
