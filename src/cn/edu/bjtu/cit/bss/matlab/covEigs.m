%
% Calculate first k eigenvalues and eigenvectors for a complex covariance
% matrix.
%
% Covr:     real part of the covariance matrix
% Covi:     imaginary part of the covariance matrix
% k:        number of eigenvalues
%
% ed:       eigenvalues
% Ev:       eigenvectors
% flag:     zero if converged
%

function [ed,Ev,flag]=covEigs(Covr,Covi,k)

%% invocate eigs
Cov=Covr+Covi*1i;

% symmetric matrix
options.issym=1;
% display nothing
options.disp=0;
[Ev,Ed,flag]=eigs(Cov,k,'lm',options);

%% extract sorted eigenvalues
ed=zeros(1,size(Ed,2));
for j=1:length(ed)
    ed(j)=real(Ed(j,j));
end

[ed,edidx]=sort(ed,'descend');

%% rearrange eigenvectors
Ev2=zeros(size(Ev));
for j=1:size(Ev2,2) 
    Ev2(:,j)=Ev(:,edidx(j));
end

Ev=Ev2;
