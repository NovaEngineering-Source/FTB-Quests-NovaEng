package com.feed_the_beast.ftbquests.net.edit;

import com.feed_the_beast.ftblib.lib.io.DataIn;
import com.feed_the_beast.ftblib.lib.io.DataOut;
import com.feed_the_beast.ftblib.lib.net.MessageToClient;
import com.feed_the_beast.ftblib.lib.net.NetworkWrapper;
import com.feed_the_beast.ftbquests.client.ClientQuestFile;
import com.feed_the_beast.ftbquests.quest.ChangeProgress;
import com.feed_the_beast.ftbquests.quest.QuestData;
import com.feed_the_beast.ftbquests.quest.QuestObjectBase;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author LatvianModder
 */
public class MessageChangeProgressResponse extends MessageToClient
{
	private int team;
	private int id;
	private ChangeProgress type;
	private boolean notifications;

	public MessageChangeProgressResponse()
	{
	}

	public MessageChangeProgressResponse(int t, int i, ChangeProgress ty, boolean n)
	{
		team = t;
		id = i;
		type = ty;
		notifications = n;
	}

	@Override
	public NetworkWrapper getWrapper()
	{
		return FTBQuestsEditNetHandler.EDIT;
	}

	@Override
	public void writeData(DataOut data)
	{
		data.writeInt(team);
		data.writeInt(id);
		ChangeProgress.NAME_MAP.write(data, type);
		data.writeBoolean(notifications);
	}

	@Override
	public void readData(DataIn data)
	{
		team = data.readInt();
		id = data.readInt();
		type = ChangeProgress.NAME_MAP.read(data);
		notifications = data.readBoolean();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void onMessage()
	{
		QuestObjectBase object = ClientQuestFile.INSTANCE.getBase(id);

		if (object != null)
		{
			QuestData t = ClientQuestFile.INSTANCE.getData(team);

			if (t != null)
			{
				object.forceProgress(t, type, notifications);
			}
		}
	}
}