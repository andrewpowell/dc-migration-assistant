import React, { FunctionComponent, useState } from 'react';
import SectionMessage from '@atlaskit/section-message';
import { Checkbox } from '@atlaskit/checkbox';
import Button from '@atlaskit/button';
import { useHistory } from 'react-router-dom';
import { dbPath } from '../../utils/RoutePaths';

export const WarningStagePage: FunctionComponent = () => {
    const history = useHistory();

    const [agreed, setAgreed] = useState<boolean>(false);

    const handleClick = () => {
        history.push(dbPath);
    };

    const agreeOnClick = (event: any) => {
        setAgreed(event.target.checked);
    };
    return (
        <div>
            <h1>Step 5 of 7: Redirect users</h1>
            <p>
                Take your Jira instance offline to prevent users from accessing it. This will allow
                us to sync your database properly. User access during this phase could prevent some
                data from being copied.
            </p>
            <SectionMessage appearance="info" title="To take your Jira instance offline:">
                <ol>
                    <li>Make sure that users are logged out</li>
                    <li>Redirect the DNS to a maintenance page</li>
                </ol>
            </SectionMessage>
            <Checkbox
                value="agree"
                label="I'm ready for the next step"
                onChange={agreeOnClick}
                name="agree"
            />
            <Button isDisabled={!agreed} onClick={handleClick}>
                Continue
            </Button>
        </div>
    );
};
