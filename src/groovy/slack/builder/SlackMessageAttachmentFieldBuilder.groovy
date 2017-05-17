package slack.builder

import slack.exception.SlackMessageException
import slack.SlackMessageAttachmentField

class SlackMessageAttachmentFieldBuilder {

	private SlackMessageAttachmentField fie

	SlackMessageAttachmentFieldBuilder() {
		fie = new SlackMessageAttachmentField()
	}

	SlackMessageAttachmentField getField() throws SlackMessageException {
		if (!fie.validate()) {
			throw new SlackMessageException("Validation errors in SlackMessageAttachmentField : " + fie.errors.allErrors.field, fie)
		}
		return fie
	}

	void title(String title) {
		fie.title = title
	}

	void value(String value) {
		fie.value = value
	}

	void isShort(boolean isShort) {
		fie.isShort = isShort
	}

}