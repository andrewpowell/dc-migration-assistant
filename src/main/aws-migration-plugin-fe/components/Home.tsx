import React, { ReactElement } from 'react';
import Button from '@atlaskit/button';
import styled from 'styled-components';

type HomeProps = {
    title: string;
    synopsis: string;
    exploreMigrationButtonText: string;
};

const HomeContainer = styled.div`
    display: flex;
    flex-direction: column;
    align-items: center;
`;

const ButtonContainer = styled.div`
    margin-top: 250px;
    align-self: flex-end;
`;

export const Home = ({ title, synopsis, exploreMigrationButtonText }: HomeProps): ReactElement => {
    return (
        <HomeContainer>
            <h2>{title}</h2>
            <p>{synopsis}</p>
            <ButtonContainer>
                <Button appearance="primary">{exploreMigrationButtonText}</Button>
            </ButtonContainer>
        </HomeContainer>
    );
};
