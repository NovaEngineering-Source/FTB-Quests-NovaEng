package com.feed_the_beast.ftbquests.net;

import com.feed_the_beast.ftblib.lib.io.DataIn;
import com.feed_the_beast.ftblib.lib.io.DataOut;
import com.feed_the_beast.ftblib.lib.net.MessageToClient;
import com.feed_the_beast.ftblib.lib.net.NetworkWrapper;
import com.feed_the_beast.ftbquests.client.ClientQuestFile;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author LatvianModder
 */
public class MessageDeleteTeamData extends MessageToClient
{
	public int team;

	public MessageDeleteTeamData()
	{
	}

	public MessageDeleteTeamData(int t)
	{
		team = t;
	}

	@Override
	public NetworkWrapper getWrapper()
	{
		return FTBQuestsNetHandler.GENERAL;
	}

	@Override
	public void writeData(DataOut data)
	{
		data.writeInt(team);
	}

	@Override
	public void readData(DataIn data)
	{
		team = data.readInt();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void onMessage()
	{
		if (ClientQuestFile.exists() && ClientQuestFile.INSTANCE.removeData(team) == ClientQuestFile.INSTANCE.self)
		{
			ClientQuestFile.INSTANCE.self = null;
		}
	}
}